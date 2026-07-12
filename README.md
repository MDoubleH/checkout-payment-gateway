# Checkout.com Payment Gateway

A small Spring Boot payment gateway that validates merchant card-payment requests, forwards valid
requests to the supplied acquiring-bank simulator, stores safe payment results in memory, and lets
merchants retrieve those results by UUID.

The implementation deliberately focuses on the challenge requirements and avoids production
infrastructure that would obscure the core flow.

## Requirements

- Eclipse Temurin or another JDK 17 distribution
- Docker and Docker Compose for the supplied bank simulator and containerized demo

## Architecture and request flow

```text
Merchant JSON
  -> PaymentGatewayController (Jackson binding + Bean Validation)
  -> PaymentGatewayService
  -> AcquiringBankClient (one synchronous POST)
  -> bank simulator
  -> Authorized or Declined
  -> immutable safe Payment
  -> ConcurrentHashMap repository
  -> safe PaymentResponse
```

The controller owns HTTP behavior, the service owns payment orchestration, the bank client owns the
external wire contract, and the repository owns in-memory storage. Separate merchant and bank DTOs
prevent the full card details from leaking into stored or returned models.

## API

The gateway runs on `http://localhost:8090`.

### Process a payment

`POST /payments`

```json
{
  "card_number": "4111111111111111",
  "expiry_month": 12,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 1050,
  "cvv": "123"
}
```

A valid request sent to the bank creates a payment resource whether the outcome is Authorized or
Declined. The response is `201 Created`, includes a `Location: /payments/{id}` header, and contains
only the safe merchant representation:

```json
{
  "id": "617b6651-2f2a-41e7-a565-396dd59254b8",
  "status": "Authorized",
  "card_number_last_four": "1111",
  "expiry_month": 12,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 1050
}
```

### Retrieve a payment

`GET /payments/{id}` returns the same safe representation with `200 OK`. A valid but unknown UUID
returns `404`; a malformed UUID returns `400`.

### Errors

Errors use one sanitized envelope. Validation errors also carry the business status `Rejected` and
field details:

```json
{
  "status": "Rejected",
  "code": "PAYMENT_REJECTED",
  "message": "Payment request is invalid",
  "request_id": "f2ee3761-b660-48a3-a9f2-27df03bffbb5",
  "field_errors": [
    {"field": "amount", "message": "amount must be positive"}
  ]
}
```

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `PAYMENT_REJECTED` | Invalid or malformed merchant payment request; bank is not called |
| 400 | `INVALID_PAYMENT_ID` | Retrieval ID is not a UUID |
| 404 | `PAYMENT_NOT_FOUND` | No stored payment has the supplied UUID |
| 502 | `ACQUIRER_INVALID_RESPONSE` | Bank response is malformed or violates its contract |
| 503 | `ACQUIRER_UNAVAILABLE` | Bank returned 503 or could not be reached |
| 504 | `ACQUIRER_TIMEOUT` | Bank call timed out |
| 500 | `INTERNAL_ERROR` | Sanitized unexpected failure |

Bank failures are technical errors—not Authorized, Declined, or Rejected payment outcomes—and are
never stored.

Every HTTP response includes a server-generated `X-Request-ID`. Client-supplied request IDs are
ignored.

## Validation

| Field | Rules |
|---|---|
| `card_number` | Required, 14–19 ASCII digits |
| `expiry_month` | Required, 1–12 |
| `expiry_year` | Required; month/year must be current or future |
| `currency` | Required; exactly `GBP`, `EUR`, or `USD` |
| `amount` | Required positive integer in the minor currency unit |
| `cvv` | Required, 3–4 ASCII digits |

Expiry is compared as a UTC `YearMonth`. A card remains valid throughout its printed expiry month.
Validation failures occur before the bank client or repository is called.

## Security boundaries

- Full PAN and CVV exist only in the inbound request and transient bank request.
- Only the last four card digits are stored and returned, as a `String` to preserve leading zeroes.
- Authorization codes are validated when required by the bank response, then discarded.
- Request bodies, PAN, CVV, authorization codes, and upstream bodies are never logged.
- Merchant-facing errors never expose exception messages or stack traces.

This exercise does not claim production PCI DSS compliance; a production gateway would tokenize
card data before it reached this application boundary.

## Observability and health

Logs use consistent `key=value` events and include the request ID through MDC. The filter removes
the MDC value in a `finally` block so pooled servlet threads cannot leak correlation context.

Basic application health is available from the gateway on port `8090`:

```bash
curl -fsS http://localhost:8090/actuator/health
```

Port `8080` belongs to the bank simulator and does not expose the gateway's Actuator endpoint.

Health does not actively probe the acquiring bank. A bank outage therefore does not mark every
gateway instance unhealthy; payment attempts receive a controlled 503 instead.

Swagger UI is available at `http://localhost:8090/swagger-ui/index.html` and the OpenAPI document at
`http://localhost:8090/v3/api-docs`.

## Configuration

| Property | Environment variable | Default |
|---|---|---|
| `bank.base-url` | `BANK_BASE_URL` | `http://localhost:8080` |
| `bank.connect-timeout` | `BANK_CONNECT_TIMEOUT` | `2s` |
| `bank.read-timeout` | `BANK_READ_TIMEOUT` | `5s` |

There are no automatic retries. A timeout can mean the bank processed the payment but its response
was lost, so an automatic retry could create a duplicate charge.

## Running locally

Start only the bank simulator:

```bash
docker compose up bank_simulator
```

Run the gateway from another terminal:

```bash
./gradlew bootRun
```

Or build and run both services as containers:

```bash
docker compose up --build
```

Ports:

- `8090`: payment gateway
- `8080`: bank simulator
- `2525`: Mountebank administration

## Testing and packaging

```bash
./gradlew clean test
./gradlew bootJar
docker compose config
docker compose up --build
```

The automated suite covers request validation, deterministic expiry behavior, Authorized and
Declined flows, safe retrieval, repository behavior, bank request mapping, representative bank
failures, error responses, request-ID cleanup, and basic health.

## Demonstration

Use an expiry date in the future and vary the final card digit:

1. POST a card ending in `1` to demonstrate Authorized and capture its `id`.
2. GET `/payments/{id}` to show the stored safe representation.
3. POST a card ending in `2` to demonstrate Declined is also stored.
4. POST a short card number to demonstrate Rejected with no bank call.
5. POST a card ending in `0` to demonstrate bank 503 maps to a technical 503 error.
6. Open Swagger UI and `/actuator/health`.

## Assumptions and limitations

- Storage is in memory, is lost on restart, and is not shared across multiple instances.
- Idempotency is not implemented. Production requires a durable idempotency key, request
  fingerprinting, concurrency handling, retention rules, and acquiring-bank cooperation.
- There is no retry, reconciliation workflow, authentication, authorization, rate limiting,
  database, Luhn validation, circuit breaker, distributed tracing, or custom business metrics.
- A production service would require tokenization, durable encrypted storage where appropriate,
  audited access controls, secrets management, monitoring, and operational runbooks.
- Request-ID MDC propagation covers this synchronous MVC flow; asynchronous propagation is outside
  this challenge.
