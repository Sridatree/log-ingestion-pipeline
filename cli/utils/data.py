# utils/data.py
import re
import ipaddress
from typing import Optional
import logging

import numpy as np
import pandas as pd
from rapidfuzz import process

from constants import VALID_CATEGORIES, OVERRIDES, CSV_SEPARATOR, CSV_ENCODING
from utils.timing import measure_time

logging.basicConfig(level=logging.INFO) 
logger = logging.getLogger("__name__")


def _clean_str(s: str) -> str:
    return re.sub(r"[^a-zA-Z]", "", s.strip().lower())


def normalize_category(category: str) -> Optional[str]:
    if not isinstance(category, str):
        # keep None to allow dropna() later
        return None

    cleaned = _clean_str(category)
    if cleaned in VALID_CATEGORIES:
        return cleaned

    if cleaned in OVERRIDES:
        return OVERRIDES[cleaned]

    # fuzzy match against valid categories (as a list)
    match, score, _ = process.extractOne(cleaned, list(VALID_CATEGORIES))
    if score >= 80:
        return match

    # return cleaned (still normalized) to make downstream behavior predictable
    return cleaned


def is_valid_ip(ip: str) -> bool:
    """True if ip is a valid IPv4/IPv6 string."""
    if not isinstance(ip, str):
        return False
    try:
        ipaddress.ip_address(ip.strip())
        return True
    except ValueError:
        return False


def _print_df(label: str, df: pd.DataFrame) -> None:
    """Pretty-print a DataFrame fully, safely."""
    logger.info(f"\n===== {label} (count={len(df)}) =====")
    if df.empty:
        print("(none)")
        return
    with pd.option_context(
        "display.max_rows", None,
        "display.max_columns", None,
        "display.width", 200,
        "display.max_colwidth", None,
    ):
        print(df)


def preprocess(file_path: str, filter_expr: str | None) -> pd.DataFrame:
    # Load
    df = pd.read_csv(file_path, sep=CSV_SEPARATOR, encoding=CSV_ENCODING)

    # Normalize/rename
    if "category" in df.columns:
        df["category"] = df["category"].apply(normalize_category)
    else:
        raise KeyError("Input CSV must contain a 'category' column.")

    if "ip" not in df.columns:
        raise KeyError("Input CSV must contain an 'ip' column.")

    if "asset_name" in df.columns and "asset" not in df.columns:
        df = df.rename(columns={"asset_name": "asset"})

    # ------- 1) Drop invalid IP rows (and print them) -------
    invalid_ip_mask = ~df["ip"].apply(is_valid_ip)
    invalid_ip_rows = df[invalid_ip_mask].copy()
    if not invalid_ip_rows.empty:
        _print_df("Rows dropped due to INVALID IP", invalid_ip_rows)

    df = df[~invalid_ip_mask].copy()

    # Remove unused columns (if present)
    df.drop(columns=["created_utc", "source"], errors="ignore", inplace=True)

    # ------- 2) Drop null/empty rows (and print them) -------
    # Normalize empties -> NaN
    df.replace("", np.nan, inplace=True)

    # Any NaN in any column makes the row droppable
    null_mask = df.isna().any(axis=1)
    null_rows = df[null_mask].copy()
    if not null_rows.empty:
        _print_df("Rows dropped due to NULL/EMPTY values", null_rows)

    df_clean = df[~null_mask].copy()

    # Optional equality filter: "column=value"
    if filter_expr:
        if "=" not in filter_expr:
            raise ValueError("Filter must be in the form key=value")
        key, value = filter_expr.split("=", 1)
        key = key.strip()
        value = value.strip()
        if key not in df_clean.columns:
            raise KeyError(f"Filter key '{key}' not in DataFrame columns: {list(df_clean.columns)}")
        df_clean = df_clean[df_clean[key] == value].copy()

    # Final visibility
    _print_df("Preview of CLEANED DataFrame (first 10 rows)", df_clean.head(10))

    return df_clean