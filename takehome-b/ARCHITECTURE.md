# Architecture Overview

## 1. Unique Identity & Concurrency (`external_id`)
- **Database Enforcement**: `transactions.external_id` has a unique index (`UNIQUE INDEX`).
- **Concurrent Writes**: Protected by database-level unique constraints. When two overlapping threads attempt to insert the same `external_id`, the database constraint violation exception is caught and handled gracefully, returning **409 Conflict** along with the existing stored transaction record.

## 2. Tables & Models
- **Transactions Table (`transactions`)**: Stores core transaction headers (`id`, `external_id`, `type`, `currency`, `occurred_at`, `total`, `metadata_json`).
- **Taxes & Fees (`transaction_tax_lines`, `transaction_fee_lines`)**: First-class child tables linked via foreign key to transactions, keeping tax rates/amounts and fee amounts fully structured and queryable rather than mashed into JSON blobs.
- **Metadata**: Type-specific metadata (`flight`, `hotel`, `rail`, `navan_fee`) is validated upon ingestion against strict schema rules and stored as JSON, ensuring full round-tripping on GET requests.

## 3. Invoice Snapshotting & Immutability
- **Snapshot Table (`invoice_items`)**: When an invoice is created, all attributes of the included transactions (totals, tax lines, fee lines, metadata) are deep-copied into immutable snapshot rows.
- **Immutability Safeguards**:
  - `PATCH` or `DELETE` on any transaction that is referenced by an active invoice returns **409 Conflict** and modifies nothing.
  - Updating an invoice via `PATCH /invoices/{id}` replaces the snapshot lines with fresh snapshots from current transactions in an atomic write.

## 4. High Volume & Performance
- **Indexes**:
  - Unique index on `transactions(external_id)`.
  - Index on `invoice_transactions(transaction_id)` to make "is this transaction on an invoice?" checks O(1) fast during transaction updates and deletes.
- **Pagination**: `GET /transactions` and `GET /invoices` require pagination parameters (`limit` capped at 100, `offset`), preventing unbounded table scans under high traffic.
