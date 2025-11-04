import pandas as pd
import numpy as np
import re
import functools
import random
import time
import asyncio
import httpx
from rapidfuzz import process, fuzz
import argparse
import ipaddress



VALID_CATEGORIES = {
    "contentinjection",
    "drivebycompromise",
    "exploitpublicfacingapplication",
    "externalremoteservices",
    "hardwareadditions",
    "phishing",
    "replicationthroughremovablemedia",
    "supplychaincompromise",
    "trustedrelationship",
    "validaccounts",
}
OVERRIDES = {
    "compromise (driveby)": "drivebycompromise",
    "compromisedriveby": "drivebycompromise"
    }

def retry(exceptions, max_retries=10, base_delay=1):
    """
    A decorator that retries a function call a number of times with an exponential backoff.
    """
    def decorator_retry(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            attempt = 0
            while True:
                try:
                    return func(*args, **kwargs)
                except exceptions:
                    if attempt >= max_retries:
                        raise
                    delay = (base_delay * (2**attempt)) + random.uniform(0, 0.1)
                    time.sleep(delay)
                    attempt += 1

        return wrapper

    return decorator_retry

def measure_time(func):
    """
    Decorator that measures and prints the execution time of a function.
    """
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        execution_time = end_time - start_time
        print(f"{func.__name__} took {execution_time:.4f} seconds to execute")
        return result
    return wrapper

@retry(Exception)
async def send_batch(client, batched_df):
    batched_df = batched_df.where(pd.notnull(batched_df), None)
    json_payload = {
        "activityRecordList": batched_df.to_dict(orient="records")
    }
    headers = {
        "Authorization": "eye-am-hiring",
        "Content-Type": "application/json"
    }
    try:
        response = await client.post("http://localhost:8080/private/v1/ingest", json=json_payload, headers=headers)
        print("Batch sent")
        response.raise_for_status()
        print(f"[SUCCESS] Sent batch of {len(batched_df)} records → {response.status_code}")
        return response
    except httpx.HTTPStatusError as exc:
        print(f"HTTP error occurred: {exc}")
        print(f"Request URL: {exc.request.url}")
        print(f"Request body: {exc.request.content.decode()}")
        print(f"Response status: {exc.response.status_code}")
        print(f"Response body: {exc.response.text}")

    except Exception as exc:
        print("An unexpected error occurred:")
        import traceback
        traceback.print_exc()


async def process_batches(df: pd.DataFrame, batch_size:int = 200):
    sem = asyncio.Semaphore(10)
    async with httpx.AsyncClient() as client:
        async def bounded_send(batched_df):
            async with sem:
                await send_batch(client, batched_df)
        tasks = []
        for i in range(0, len(df), batch_size):
                batched_df = df.iloc[i:i+batch_size]
                tasks.append(asyncio.create_task(bounded_send(batched_df)))
        await asyncio.gather(*tasks)
    
 
def normalize_category(category: str) -> str:
    if not isinstance(category, str):
        print("category is not a string")
        return None

    category = category.strip().lower()
    cleaned_category = re.sub(r'[^a-zA-Z]', '', category)
    if cleaned_category in VALID_CATEGORIES:
        return cleaned_category
    if cleaned_category in OVERRIDES:
        print(f"old :: {cleaned_category} new:: {OVERRIDES[cleaned_category]}")
        return OVERRIDES[cleaned_category]
    match, score, _ = process.extractOne(cleaned_category, VALID_CATEGORIES)
    if score >= 80:
        
        return match
    return cleaned_category



def is_valid_ip(ip: str) -> bool:
    """Check if the IP address is valid IPv4 or IPv6."""
    if not isinstance(ip, str):
        return False
    try:
        ipaddress.ip_address(ip.strip())
        return True
    except ValueError:
        return False
@measure_time
def preprocess(file, filter):
    
    df = pd.read_csv(file,sep=";")
    print(df.head())
    # Apply normalization
    df["category"] = df["category"].apply(normalize_category)
    df = df.rename(columns={"asset_name":"asset"})
    invalid_count = (~df["ip"].apply(is_valid_ip)).sum()
    if invalid_count > 0:
        print(f"Dropping {invalid_count} rows with invalid IP addresses")
    df = df[df["ip"].apply(is_valid_ip)].copy()
    df.drop("created_utc",axis=1)
    df.drop("source",axis =1)
    df.replace('', np.nan, inplace=True)
    df_clean = df.dropna()
    # Apply filter if provided
    if filter:
        key, value = filter.split("=", 1)
        df_clean = df_clean[df_clean[key] == value]
    return df_clean

@measure_time
def main():
    
    parser = argparse.ArgumentParser(description="CLI to send CSV data to MS endpoint")
    parser.add_argument("--file", required=True, help="Path to input CSV file")
    parser.add_argument("--filter", help="Optional filter condition, e.g., category=server")
    args = parser.parse_args()
    df_cleaned = preprocess(args.file, args.filter)
    asyncio.run(process_batches(df_cleaned))
    print("[DONE] All batches sent successfully ✅")



if __name__ == "__main__":
    main()
    
    #"C:\\Users\\91700\\Downloads\\example_data_2.csv"




#tbd : add auth header 