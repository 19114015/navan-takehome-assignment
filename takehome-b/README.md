# Navan Take-home B — Transactions and Invoices API

Java & Spring Boot application backed by SQLite providing a robust commerce transactions and invoicing engine.

## Requirements
- Java 17+
- Maven 3.8+

## How to Run

1. **Build and run tests**:
   ```bash
   ./mvnw clean test
   ```

2. **Run the API**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The service listens on `http://localhost:8080`. SQLite database file `navan.db` is created in the working directory.

---

## Example Curls

### 1. Create a Flight Transaction
```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.0,
    "tax_lines": [{ "name": "Airport tax", "amount": 15.0 }],
    "fee_lines": [{ "name": "YQ", "amount": 25.0 }],
    "metadata": {
      "origin": "TXL",
      "destination": "LHR",
      "fare": 180.0,
      "coupons": [
        {
          "carrier": "BA",
          "flight_number": "BA982",
          "from": "TXL",
          "to": "LHR",
          "departure": "2026-03-11T07:00:00Z",
          "arrival": "2026-03-11T08:20:00Z"
        }
      ]
    }
  }'
```

### 2. Duplicate external_id (Returns 409 Conflict with existing record)
Posting the same payload above a second time returns **409 Conflict** along with the existing stored transaction object and ID.

### 3. Create an Invoice by Transaction ID
```bash
curl -X POST http://localhost:8080/invoices \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "txn_12345678"
  }'
```

### 4. Paginated List of Transactions
```bash
curl "http://localhost:8080/transactions?limit=50&offset=0"
```
