# LogProcessor - Complete Data Ingestion & Processing Pipeline

A comprehensive, high-performance, scalable solution for ingesting, enriching and processing malicious activity logs. This project consists of two main components:
1. **Python CLI Tool** - Preprocesses CSV data and sends it to the ingestion endpoint
2. **Spring Boot Microservice** - Processes, enriches, and analyzes the ingested data

---

## Table of Contents

- [Overview](#overview)
- [Complete Architecture](#complete-architecture)
- [Components](#components)
    - [1. CSV Ingestion CLI (Python)](#1-csv-ingestion-cli-python)
    - [2. LogProcessor Microservice (Spring Boot)](#2-logprocessor-microservice-spring-boot)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Detailed Usage](#detailed-usage)
- [API Documentation](#api-documentation)
- [Monitoring & Metrics](#monitoring--metrics)
- [Error Handling](#error-handling)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

---

## Overview

LogProcessor is a distributed, fault-tolerant system designed to handle high-throughput log processing with a focus on reliability and resilience. The complete pipeline:

1. **Ingestion**: Python CLI reads, cleans, validates, and batches CSV data
2. **Processing**: Spring Boot service ingests records into RabbitMQ
3. **Enrichment**: Records are enriched with external enrichment service
4. **Analytics**: Batched records are sent to analytics service for analytics

---


## Complete Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          CSV INGESTION LAYER (Python)                        │
└────────────────────────────────────┬─────────────────────────────────────────┘
                                     │
                                     │ 1. Read CSV
                                     │ 2. Clean & Validate
                                     │ 3. Batch & Send (async)
                                     │
                                     ▼
                          POST /private/v1/ingest
                                     │
┌────────────────────────────────────┼─────────────────────────────────────────┐
│              LOGPROCESSOR MICROSERVICE (Spring Boot)                         │
│                                    │                                         │
│                    ┌───────────────▼──────────────┐                          │
│                    │  LogProcessor Controller     │                          │
│                    │  (Validation & Ingestion)    │                          │
│                    └───────────────┬──────────────┘                          │
│                                    │ Publishes to RabbitMQ                   │
│                                    ▼                                         │
│            ┌────────────────────────────────────────────────┐                │
│            │          RabbitMQ Exchange                     │                │
│            └───┬────────────────────────────────────────┬───┘                │
│                │                                        │                    │
│                ├──► raw-records-queue                   └──►dead-letter-queue│
│                │         │                                                   │
│                │         ▼                                                   │
│                │    ┌─────────────────────┐                                  │
│                │    │ EnrichmentConsumer  │                                  │
│                │    │ (4-8 threads)       │                                  │
│                │    └──────────┬──────────┘                                  │
│                │               │ Calls Enrichment API                        │
│                │               │ (Circuit Breaker + Retry)                   │
│                │               ▼                                             │
│                │         enriched-records-queue                              │
│                │               │                                             │
│                │               ▼                                             │
│                │    ┌─────────────────────┐                                  │
│                │    │ AnalyticsConsumer   │                                  │
│                │    │ (Batch: 20 records) │                                  │
│                │    └──────────┬──────────┘                                  │
│                │               │ Calls Analytics API                         │
│                │               │ (Rate Limited: 1 req/10s)                   │
│                │               ▼                                             │
│                │         Analytics Service                                   │
│                │                                                             │
└──────────────────────────────────────────────────────────────────────────────┘
```
## Where my brain was at

The most interesting part of this project was to hit a sweet spot with concurrency so the processing time would always be the time taken by the analytics service to process the records.(Our hands are tied here since Analytics api is rate limited at 2 records/s)

So, data ingestion and enrichment will always need to be ahead of the analytics, so 20 records would always be ready for the analytics service for processing every 10 seconds.

While maintaining a list or a queue first came to me, eventually, considering concurrent calls happening writing into and reading off the queue, RabbitMQ seemed to be an easier alternative compared to using significant amount of locking and/or synchronization within the code.

Once I decided to proceed with Rabbit, usage of DLQ for records that failed maximum retries seemed only natural.

## Components

### 1. CSV Ingestion CLI (Python)

A configurable Python CLI tool that preprocesses CSV data and sends it asynchronously to the LogProcessor ingestion endpoint.

#### Features

**Data Preprocessing**
- Cleans and normalizes category names
- **Fuzzy matching** (rapidfuzz) to intelligently detect and fix typos in categories
- Validates **IPv4 and IPv6** addresses using the standard library
- Drops invalid or null rows safely and prints full details of what was dropped
- Drops redundant columns not needed by the Analytics API
- Allows column filtering (`--filter key=value`)

**Networking & Robustness**
- Uses **httpx** with tuned connection pooling and configurable timeouts, retries with exponential backoff and jitter
- Gracefully handles large datasets using **asynchronous batching**
- Configurable concurrency control to prevent server overload

#### CLI Installation

```bash
cd /path/to/cli
python -m venv .venv
source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pip install -r requirements.txt
```

#### CLI Usage

**Basic Example:**
```bash
python main.py --file /path/to/data.csv
```

**Optional Parameters:**

| Flag            | Description                                           | Default      |
| --------------- | ----------------------------------------------------- | ------------ |
| `--file`        | Path to the input CSV file                            | *Required*   |
| `--filter`      | Keep only rows matching column=value after cleaning   | None         |
| `--batch-size`  | Number of rows per network batch                      | 200          |
| `--concurrency` | Number of simultaneous HTTP requests                  | 10           |

**Examples:**

```bash
# 1️⃣ Basic ingestion
python main.py --file ./data/example.csv

# 2️⃣ Filter only rows with category 'phishing'
python main.py --file ./data/example.csv --filter "category=phishing"

# 3️⃣ Tune concurrency and batch size
python main.py --file ./data/example.csv --batch-size 500 --concurrency 5
```

#### CLI Output Example

```
===== Rows dropped due to INVALID IP (count=3) =====
<full rows printed>
===== Rows dropped due to NULL/EMPTY values (count=2) =====
<full rows printed>
Preview of CLEANED DataFrame (first 10 rows)
[SUCCESS] Sent batch of 200 records → 200
[SUCCESS] Sent batch of 200 records → 200
[DONE] All batches sent successfully ✅
```
---
### 2. LogProcessor Microservice (Spring Boot)

A high-performance, resilient microservice for ingesting, enriching, and processing malicious activity logs using Spring Boot, RabbitMQ, and Resilience4j.

#### Features

**1. High-Performance Ingestion**
- REST API endpoint for record ingestion
- Validation of IP addresses and security categories
- Immediate publishing to RabbitMQ for async processing

**2. Intelligent Enrichment**
- Concurrent processing with configurable thread pools (4-8 threads)
- Circuit breaker protection (opens at 60% failure rate)
- Exponential backoff retry (up to 4 attempts)
- Bulkhead pattern to limit concurrent calls

**3. Rate-Limited Analytics**
- Strict batching (max 20 records per batch)
- Rate limiting (1 batch per 10 seconds)
- Automatic retry for transient errors (excludes 429 errors)
- Execution time tracking with AOP

---

## Technology Stack

### CLI Tool (Python)
- **Python 3.8+**
- **httpx** - Async HTTP client
- **pandas** - Data processing
- **rapidfuzz** - Fuzzy string matching
- **argparse** - CLI argument parsing

### Microservice (Java)
- **Java 17**
- **Spring Boot 3.5.7**
- **Spring AMQP** - RabbitMQ integration
- **Spring WebFlux** - Reactive HTTP client
- **Resilience4j** - Circuit breaker, retry, rate limiter, bulkhead
- **Jakarta Validation** - Bean validation
- **Lombok** - Boilerplate reduction
- **Spring AOP** - Aspect-oriented programming
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **SpringDoc OpenAPI** - API documentation

---

## Prerequisites

### For CLI Tool
- Python 3.8 or higher
- pip package manager

### For Microservice
- Java 17 or higher
- Maven 3.6+
- RabbitMQ 3.x (running on localhost:5672 or configure in application.yml)
- Access to external Enrichment and Analytics APIs

---

## Installation

### 1. Install CLI Tool

```bash
cd /path/to/cli
python -m venv .venv
source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pip install -r requirements.txt
```

### 2. Install RabbitMQ

```bash

# install locally
# Windows: net start RabbitMQ
# Linux: sudo systemctl start rabbitmq-server
# Go to http://localhost:15672/ and login with username: guest and password: guest
```

### 3. Build LogProcessor Microservice

```bash
git clone <repository-url>
cd LogProcessor
mvn clean install
```

---

## Quick Start

### Step 1: Start RabbitMQ

```bash
# Verify RabbitMQ is running
# Management UI: http://localhost:15672 (guest/guest)
```

### Step 2: Start LogProcessor Microservice

```bash
cd LogProcessor
mvn spring-boot:run

# Or using the JAR
mvn clean package
java -jar target/LogProcessor-0.0.1-SNAPSHOT.jar
```

### Step 3: Run CLI to Ingest Data

```bash
# Activate virtual environment
source .venv/bin/activate

# Ingest CSV data
python main.py --file ./data/sample.csv --batch-size 200
```

### Step 4: Monitor Progress

- **RabbitMQ Management UI**: http://localhost:15672
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Application Logs**: `logs/application.log`

---



## API Documentation

### Ingest Endpoint

**POST** `/private/v1/ingest`

Ingests a batch of activity records for processing.

#### Request Headers

```
Authorization: <token>
Content-Type: application/json
```

#### Request Body

```json
{
  "activityRecordList": [
    {
      "id": 1,
      "asset": "server-01",
      "ip": "192.168.1.100",
      "category": "phishing"
    },
    {
      "id": 2,
      "asset": "workstation-05",
      "ip": "10.0.0.50",
      "category": "exploitpublicfacingapplication"
    }
  ]
}
```

#### Success Response (200 OK)

```json
{
  "recordsIngested": 2,
  "message": "Records Ingested to raw queue"
}
```

#### Validation Error Response (400 Bad Request)

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "activityRecordList[0].ip": "Invalid IP address",
    "activityRecordList[1].category": "Invalid category"
  },
  "timeStamp": "2025-11-04T10:30:00"
}
```

### OpenAPI Documentation

Once the application is running, access the interactive API documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

---

## Monitoring & Metrics

### CLI Metrics

The CLI provides execution time metrics for:
- Data loading
- Preprocessing
- Network sending
- Total runtime

### Microservice Metrics

The MetricsService tracks:

**Enrichment Metrics**
- Records enriched successfully
- Enrichment failures
- Average enrichment time
- Total enrichment time

**Analytics Metrics**
- Batches processed
- Records sent to analytics
- Analytics failures
- Average batch processing time
- Total batch processing time

---

## Error Handling

### CLI Error Handling

- **Network Errors**: Automatic retry with exponential backoff
- **Validation Errors**: Logged and dropped rows are displayed
- **Timeout Errors**: Configurable timeouts with retry logic

### Microservice Error Handling

**Custom Exceptions:**
- `AnalyticsException`: Thrown when analytics service operations fail
- `MessagePublishException`: Thrown when publishing to RabbitMQ fails
- `RateLimitExceededException`: Thrown when rate limits are exceeded

**Dead Letter Queue:**

Failed enrichment records are automatically sent to the `dead-letter-queue` with failure details:


---

## Resilience Patterns

### Circuit Breaker (Enrichment Service)
- **Sliding Window**: 20 calls
- **Failure Threshold**: 60%
- **Wait Duration**: 15 seconds
- **Half-Open State**: 5 permitted calls

When the circuit breaker opens, requests fail fast instead of waiting for timeouts.

### Retry (Enrichment Service)
- **Max Attempts**: 4
- **Initial Wait**: 300ms
- **Backoff**: Exponential (2x multiplier)
- **Sequence**: 300ms → 600ms → 1200ms

### Retry (Analytics Service)
- **Max Attempts**: 3
- **Wait Duration**: 500ms
- **No Backoff**: Fixed intervals
- **Ignores**: 429 Too Many Requests errors

### Rate Limiting (Analytics Service)
- **Limit**: 1 request per 10 seconds
- **Timeout**: 10 seconds
- **Behavior**: Waits for permission, fails after timeout

### Bulkhead (Enrichment Service)
- **Max Concurrent Calls**: 10
- **Max Wait Duration**: 500ms
- **Behavior**: Limits parallel enrichment calls

---

## Troubleshooting

### Common Issues

**Issue**: CLI can't connect to LogProcessor

**Solution**:
1. Verify LogProcessor is running: `curl http://localhost:8080/actuator/health`
2. Check endpoint URL in CLI `constants.py`
3. Verify firewall settings

---

**Issue**: Application can't connect to RabbitMQ

**Solution**:
1. Install RabbitMQ and run: `net start RabbitMQ` (Windows) or `sudo systemctl start rabbitmq-server` (Linux)
2. Verify RabbitMQ is running: http://localhost:15672

Response:
```
The RabbitMQ service is starting.
The RabbitMQ service was started successfully.
```

**To delete queues and retry:**
1. Go to http://localhost:15672/#/queues
2. Click on the queue and scroll down
3. Click on the "Delete Queue" Button

---

**Issue**: Circuit breaker constantly open

**Solution**:
- Check enrichment service health
- Increase `waitDurationInOpenState`
- Adjust `failureRateThreshold`

---

**Issue**: Rate limit errors (429) from Analytics API

**Solution**:
- Verify rate limiter configuration in `application.yml` matches API limits (1 req/10s)
- Check analytics service rate limiting

---

**Issue**: Records stuck in dead letter queue

**Solution**:
- Inspect DLQ messages for error patterns
- Fix upstream issues
- Consider reprocessing failed records

---

**Issue**: High memory usage in CLI

**Solution**:
- Reduce `--batch-size`
- Reduce `--concurrency`
- Process CSV in chunks

---

## Logging

### CLI Logs
- Real-time console output
- Execution time tracking
- Detailed error messages

### Microservice Logs

Logs are written to:
- **Console**: Real-time output
- **File**: `logs/application.log`

**Log Levels:**

Configure log levels in `application.yml`:

```yaml
logging:
  level:
    com.analytics.LogProcessor: INFO
    com.analytics.LogProcessor.service.AnalyticsService: INFO
```

---

## Project Structure

### CLI Structure

```
cli_project/
├── constants.py           # Central configuration (timeouts, retries, endpoint, etc.)
├── main.py                # CLI entry point
├── utils/
│   ├── __init__.py
│   ├── data.py            # Data loading, cleaning, and validation
│   ├── network.py         # HTTP batching, retries, and concurrency logic
│   └── timing.py          # Decorator for measuring execution time
└── requirements.txt
```

### Microservice Structure

```
LogProcessor/
├── src/
│   ├── main/
│   │   ├── java/com/analytics/LogProcessor/
│   │   │   ├── annotation/        # Custom annotations (e.g., @TrackExecutionTime)
│   │   │   ├── aspect/            # AOP aspects for cross-cutting concerns
│   │   │   ├── config/            # Configuration classes (RabbitMQ, WebClient, etc.)
│   │   │   ├── consumer/          # RabbitMQ message consumers
│   │   │   ├── controller/        # REST controllers
│   │   │   ├── model/             # Domain models and DTOs
│   │   │   ├── service/           # Business logic services
│   │   │   ├── validation/        # Custom validators
│   │   │   └── LogProcessorApplication.java
│   │   └── resources/
│   │       └── application.yml    # Application configuration
│   └── test/                      # Unit and integration tests
├── pom.xml                        # Maven dependencies
└── README.md
```

---

## Testing

### Run All Tests

```bash
cd LogProcessor
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=IngestServiceTest
```

## Future Roadmap

1. Enhance validation and error handling.
2. Monitoring Dashboards
3. Secrets onboarding(Vault?)
