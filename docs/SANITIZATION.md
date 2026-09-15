# Sanitization and domain boundaries

This repository demonstrates transferable inventory-receipt engineering skills
without reproducing any employer's implementation.

## What is intentionally generic

- `Item`, `Receipt`, and `ReceiptLine` are common supply-chain domain concepts.
- Product names describe fictional warehouse equipment and consumables.
- Item numbers such as `ITM-1001` are invented and use no real numbering scheme.
- The supplier name `Northwind Industrial Supply` is fictional.
- Quantities, receipt numbers, UUIDs, serial numbers, timestamps, and Kafka
  offsets are synthetic.
- Database and event schemas were designed specifically for this portfolio.
- API paths, validation limits, statuses, and business rules are project-specific.
- The UI was designed from scratch and does not reproduce an enterprise screen.
- Organization codes, warehouse names, locations, transfer numbers, requesters,
  dates, statuses, and quantities are invented for this repository.

## What is deliberately excluded

- Employer source code, SQL, configuration, integrations, and documentation
- Internal product names, real SKUs, serials, supplier or customer information
- Screenshots or exports from a workplace system
- Proprietary table names, API contracts, event formats, and business rules
- Production volumes, performance measurements, incidents, and architecture
- Credentials, environment names, URLs, organization names, and employee data

## Safe interview framing

Use this wording:

> I applied the general inventory and receiving concepts I learned in enterprise
> SCM work to a new, independently designed portfolio application. The code,
> schema, data, event contract, and UI are fictional and vendor-neutral.

Discuss the transferable problem—validating inbound receipt lines, identifying
items, coordinating inventory movement between organizations, persisting an
aggregate, and notifying downstream services—without describing how a previous
employer implemented it.
