
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
    "compromisedriveby": "drivebycompromise",
}

# API 
ENDPOINT_URL = "http://localhost:8080/private/v1/ingest"
AUTH_TOKEN = "eye-am-hiring"

# httpx.Timeout params (seconds)
TIMEOUT_CONNECT = 5.0
TIMEOUT_READ = 30.0
TIMEOUT_WRITE = 30.0
TIMEOUT_POOL = 5.0

# Concurrency / batching
DEFAULT_BATCH_SIZE = 200
DEFAULT_CONCURRENCY = 10

# Retry/backoff
MAX_RETRIES = 7               # total attempts = MAX_RETRIES + 1
BASE_DELAY = 0.5              # seconds
MAX_DELAY = 10.0              # cap per backoff sleep

# CSV read options (tweak as needed)
CSV_SEPARATOR = ";"
CSV_ENCODING = "utf-8"