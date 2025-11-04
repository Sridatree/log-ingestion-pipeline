import argparse
import asyncio

from constants import DEFAULT_BATCH_SIZE
from utils.data import preprocess
from utils.ingestion_api import process_batches
from utils.timing import measure_time
import logging

logging.basicConfig(level=logging.INFO) 
logger = logging.getLogger("__name__")


@measure_time
def _preprocess(file: str, filter_expr: str | None):
    return preprocess(file, filter_expr)

@measure_time
def main():
    parser = argparse.ArgumentParser(description="CLI to send CSV data to MS endpoint")
    parser.add_argument("--file", required=True, help="Path to input CSV file")
    parser.add_argument("--filter", help="Optional filter condition, e.g., category=server")
    parser.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE, help="Batch size (default: 200)")
    parser.add_argument("--concurrency", type=int, default=10, help="Number of concurrent requests (default: 10)")
    args = parser.parse_args()

    df_cleaned = _preprocess(args.file, args.filter)
    asyncio.run(process_batches(df_cleaned, batch_size=args.batch_size, concurrency=args.concurrency))
    logger.info("[DONE] All batches sent successfully ✅")


if __name__ == "__main__":
    main()