from __future__ import annotations

import asyncio
import random
import logging

import httpx
import pandas as pd

logging.basicConfig(level=logging.INFO) 
logger = logging.getLogger("__name__")


from constants import (
    ENDPOINT_URL,
    AUTH_TOKEN,
    MAX_RETRIES,
    BASE_DELAY,
    MAX_DELAY,
    TIMEOUT_CONNECT,
    TIMEOUT_READ,
    TIMEOUT_WRITE,
    TIMEOUT_POOL,
    DEFAULT_CONCURRENCY,
)

# ---- Robust async retry with exponential backoff + jitter ----

async def _async_backoff_sleep(attempt: int) -> None:
    # exponential backoff with jitter, capped
    delay = min((BASE_DELAY * (2 ** attempt)) + random.uniform(0, 0.25), MAX_DELAY)
    await asyncio.sleep(delay)


def make_http_client() -> httpx.AsyncClient:

    timeout = httpx.Timeout(
        connect=TIMEOUT_CONNECT,
        read=TIMEOUT_READ,
        write=TIMEOUT_WRITE,
        pool=TIMEOUT_POOL,
    )
    limits = httpx.Limits(
        max_keepalive_connections=100,
        max_connections=100,
    )
    return httpx.AsyncClient(
        timeout=timeout,
        limits=limits,  
        headers={"Authorization": AUTH_TOKEN, "Content-Type": "application/json"},
    )


async def send_batch(client: httpx.AsyncClient, batched_df: pd.DataFrame) -> None:
    """
    Sends a batch with retries on transient errors (read/connect timeouts, network issues, 5xx).
    """
    # Replace NaN with None for JSON
    batched_df = batched_df.where(pd.notnull(batched_df), None)

    payload = {"activityRecordList": batched_df.to_dict(orient="records")}

    last_exc: Exception | None = None
    for attempt in range(MAX_RETRIES + 1):
        try:
            resp = await client.post(ENDPOINT_URL, json=payload)
            # Raise for non-2xx
            resp.raise_for_status()
            logger.info(f"[SUCCESS] Sent batch of {len(batched_df)} records → {resp.status_code}")
            return
        except (httpx.ReadTimeout, httpx.ConnectTimeout, httpx.RemoteProtocolError, httpx.NetworkError) as exc:
            last_exc = exc
            logger.error(f"[WARN] Transient network error on attempt {attempt+1}/{MAX_RETRIES+1}: {exc!r}")
            if attempt < MAX_RETRIES:
                await _async_backoff_sleep(attempt)
                continue
            break
        except httpx.HTTPStatusError as exc:
            status = exc.response.status_code
            body_preview = exc.response.text[:500]
            logger.error(f"[HTTP] {status} on attempt {attempt+1}/{MAX_RETRIES+1}")
            # Retry 5xx; don't retry 4xx
            if 500 <= status < 600 and attempt < MAX_RETRIES:
                await _async_backoff_sleep(attempt)
                continue
            # Non-retryable or retries exhausted
            raise
        except Exception as exc:
            last_exc = exc
            logger.error(f"[ERROR] Unexpected error on attempt {attempt+1}/{MAX_RETRIES+1}: {exc!r}")
            if attempt < MAX_RETRIES:
                await _async_backoff_sleep(attempt)
                continue
            break

    # All retries exhausted
    assert last_exc is not None
    raise last_exc


async def process_batches(df: pd.DataFrame, batch_size: int = 200, concurrency: int = DEFAULT_CONCURRENCY) -> None:
    """
    Sends DataFrame rows in batches with bounded concurrency.
    """
    sem = asyncio.Semaphore(concurrency)
    async with make_http_client() as client:
        async def _bounded_send(batch: pd.DataFrame) -> None:
            async with sem:
                await send_batch(client, batch)

        tasks = []
        for i in range(0, len(df), batch_size):
            batch = df.iloc[i:i + batch_size]
            tasks.append(asyncio.create_task(_bounded_send(batch)))

        # Fail fast but still report which tasks erred
        done, pending = await asyncio.wait(tasks, return_when=asyncio.ALL_COMPLETED)
        for task in done:
            exc = task.exception()
            if exc:
                raise exc