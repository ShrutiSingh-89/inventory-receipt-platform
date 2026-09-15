# AI Receiving Copilot — Implementation, Testing, and Interview Guide

## 1. What was implemented

The AI-assisted receiving copilot turns sanitized supplier shipment data into a
reviewable receipt draft. It matches supplier descriptions to the fictional item
catalog, detects receiving exceptions, proposes missing serial numbers, and
explains why each line is ready, needs review, or is blocked.

The runnable provider is deliberately named **LOCAL_EXPLAINABLE**. It uses a
transparent scoring algorithm instead of pretending that a large language model
is connected. This gives the project a repeatable local demo and an honest
integration boundary for Oracle AI Agent Studio or another approved model.
The instructions field is part of that future agent contract; the local provider
does not interpret free-form instructions.

The most important control is:

> Analysis never changes inventory. A user reviews editable recommendations,
> prepares a receipt draft, and explicitly submits it through the normal receipt
> API. Spring Boot then validates everything again.

## 2. Run the demo step by step

The simplest route requires only Docker Desktop:

~~~bash
cd inventory-receipt-platform
docker compose up --build
~~~

For an IntelliJ-based setup, configure a project SDK of Java 17 or newer and a
Node.js interpreter of 22.12 or newer. Start PostgreSQL and Kafka first:

~~~bash
docker compose up -d postgres kafka
~~~

Then run **InventoryReceiptApplication** from IntelliJ. In a second terminal:

~~~bash
cd frontend
npm ci
npm run dev
~~~

The Docker route does not require locally installed Java, Maven, or Node.

Open [http://localhost:3000](http://localhost:3000), then:

1. Select **AI receiving copilot** in the left navigation.
2. Keep the fictional supplier and **ORG-CENTRAL**.
3. Review the three default supplier lines.
4. Keep **Allow serial proposals** enabled.
5. Select **Analyze shipment**.
6. Inspect match confidence, severity, issues, and the recommendation.
7. Edit a matched item, received quantity, or proposed serial if desired.
8. Select **Prepare receipt draft**.
9. Review the normal receipt screen. Nothing is saved yet.
10. Select **Create receipt** to invoke the authoritative Spring service.
11. Inspect the receipt status and Kafka event monitor.

The default scenario demonstrates:

- A serialized scanner line with one missing supplier serial. The copilot
  proposes an editable candidate.
- A non-serialized label line with an over-receipt. The copilot explains that a
  person must review the quantity exception.
- A serialized tablet line with the correct number of supplier serials.

## 3. End-to-end code path

### Step 1: React collects sanitized shipment data

**ReceivingCopilot** in **frontend/src/App.jsx** owns the supplier, receiving
organization, instructions, serial-generation permission, and editable shipment
rows. The input is structured deliberately; document OCR is not hidden inside
the demo.

When the user selects **Analyze shipment**, React converts comma- or
newline-separated serial text into arrays and calls:

~~~http
POST /api/copilot/receiving/analyze
Content-Type: application/json
~~~

### Step 2: Bean Validation protects the API shape

**AnalyzeShipmentRequest** validates the supplier, organization, instructions,
and nonempty line list. **ShipmentLineInput** restricts identifiers,
descriptions, quantities, and serial lengths.

These checks prevent malformed payloads from reaching recommendation logic. They
do not decide business meaning.

### Step 3: the service validates the organization

**ReceivingCopilotService** resolves the receiving organization through
**InventoryOrganizationRepository**. Unknown IDs produce a 404 response instead
of allowing the assistant to invent an organization.

### Step 4: supplier descriptions are matched

The service normalizes punctuation and case, removes common stop words, and
compares description tokens with each item's SKU, name, category, description,
and sanitized aliases.

The strongest matches are:

1. Exact SKU
2. Exact item name
3. Description or alias phrase
4. Token overlap

A candidate below the confidence threshold is rejected. This is an important
agent safeguard: an uncertain result becomes **BLOCKED**, not a guessed item.

### Step 5: business exceptions are explained

For each matched line, the service evaluates:

- expected quantity versus received quantity;
- repeated supplied serials across the entire shipment;
- whether the item is serial-controlled;
- number of serials versus received units;
- whether serial proposal is permitted.

The response uses three severities:

- **READY**: no exception was detected.
- **REVIEW**: a usable proposal exists, but a person must evaluate an exception.
- **BLOCKED**: the current values cannot form a valid receipt.

### Step 6: missing serials receive proposals

For a serial-controlled item, the service combines:

- the item's configured prefix;
- the fictional receiving organization code;
- the current UTC date;
- a stable line-derived sequence.

Example:

~~~text
SCN-CENTRAL-260915-1884
~~~

These are proposals, not reserved production serials. The user may edit them,
and receipt creation checks them again.

### Step 7: React provides an approval checkpoint

The recommendation table keeps product selection, quantities, and serials
editable. Before preparing a draft, React verifies:

- every line has an internal item;
- each received quantity is a positive integer;
- serialized items have one serial per unit;
- non-serialized items have no serials;
- serials are unique across the draft.

Client validation improves usability but is never considered authoritative.

### Step 8: the receipt service revalidates

**ReceiptService** independently verifies the serial policy, checks duplicates
inside the request, checks existing persisted serials, saves the aggregate, and
publishes **ReceiptCreated**.

The database adds the final protection:

- **receipt_line_serials** stores one row per physical serialized unit.
- A case-insensitive unique index prevents global serial reuse.
- The foreign key prevents serial rows without a receipt line.
- Cascading delete behavior keeps the aggregate consistent.

This layered approach is called defense in depth.

## 4. API example

~~~json
{
  "supplierName": "Northwind Industrial Supply",
  "organizationId": 1,
  "operatorInstructions": "Match items and explain all exceptions.",
  "allowGeneratedSerials": true,
  "lines": [
    {
      "lineId": "SUP-1",
      "supplierDescription": "Rugged handheld scanner with charging dock",
      "expectedQuantity": 3,
      "receivedQuantity": 3,
      "suppliedSerialNumbers": ["SUP-SCN-0001", "SUP-SCN-0002"]
    }
  ]
}
~~~

The response contains the matched item, confidence, severity, issues,
recommendation, proposed serial array, provider name, and guardrails.

## 5. Automated tests included now

Run:

~~~bash
cd inventory-receipt-platform/backend
mvn test
~~~

**ReceivingCopilotServiceTest** verifies:

1. A recognizable supplier description maps to the expected internal item.
2. Missing serials are proposed only when permission is enabled.
3. An unknown product is blocked instead of fabricated.
4. Case-insensitive duplicate serials across shipment lines are blocked.
5. Every recommendation requires human approval.

**ReceiptServiceTest** verifies the authoritative boundary:

1. A receipt persists and publishes an event.
2. Duplicate serials inside a request are rejected.
3. Unknown item IDs are rejected.
4. A controlled item requires one serial per unit.
5. A serial already present in inventory is rejected.

## 6. Useful manual test scenarios

### Product matching

- Enter an exact SKU such as **ITM-1001**.
- Enter the exact name.
- Enter a configured alias such as “barcode gun”.
- Change punctuation and capitalization.
- Enter an unrelated description and verify that it becomes blocked.
- Enter an ambiguous phrase such as “mobile device” and evaluate whether the
  confidence threshold should be raised.

### Quantities

- Expected and received are equal.
- Received is greater than expected.
- Received is less than expected.
- Received is zero.
- Received exceeds 10,000 and is rejected by API validation.

### Serials

- Provide exactly one unique serial per serialized unit.
- Omit one serial with proposal permission enabled.
- Omit one serial with proposal permission disabled.
- Repeat a serial on the same line.
- Repeat it using different letter case on another line.
- Add a serial to a non-controlled item.
- Add more serials than received units.
- Edit a proposed serial before preparing the draft.
- Submit a serial that was persisted on an earlier receipt.

### Human approval

- Analyze and verify that no receipt appears in the database.
- Prepare a draft and verify that it still is not persisted.
- Submit the receipt and verify that it now has an ID and audit history.
- Navigate away before submission and verify no inventory write.

### API and failure handling

- Send an unknown organization ID.
- Omit the supplier name.
- Send an empty line array.
- Stop the backend and confirm that React displays a recoverable API error.
- Submit the same valid serial concurrently and verify that the database unique
  index permits only one transaction.

## 7. Additional tests worth adding

The next testing layers would be:

- Spring MVC contract tests for status codes and JSON fields.
- Testcontainers integration tests using real PostgreSQL and Kafka.
- Playwright browser tests for analyze, edit, prepare, and confirm.
- Concurrency tests for simultaneous serial submissions.
- Performance tests with large shipment files.
- Accessibility tests for keyboard navigation and table labels.
- Security tests for authentication, organization access, and prompt injection.
- Model evaluation datasets if a real language model is connected.

For a real AI provider, maintain a sanitized golden dataset containing supplier
phrases and expected matches. Measure precision, recall, blocked-result rate,
unsupported claims, latency, and cost. Never test only whether the answer sounds
convincing.

## 8. What this project does not currently test or claim

Be explicit about these limitations in interviews:

- No Oracle AI Agent Studio tenant is connected.
- No external LLM or machine-learning model is invoked.
- Free-form agent instructions are accepted by the API contract but are not
  interpreted by the local provider.
- The local matcher is deterministic; it demonstrates the workflow and safety
  boundary, not semantic model quality.
- It does not extract tables from PDF, image, email, or EDI documents.
- Generated serials are proposals rather than a production sequence reservation
  service.
- Availability is simplified at item level, not maintained separately for every
  organization.
- Authentication, authorization, approval roles, and row-level organization
  security are not implemented.
- Recommendations are not persisted as a formal audit record.
- Kafka currently publishes receipt creation, not every copilot decision.
- The demo is not benchmarked for production shipment volumes.

These are not weaknesses to hide. They demonstrate that you understand the
difference between a portfolio workflow and a production enterprise system.

## 9. Connecting Oracle AI Agent Studio later

An Oracle agent could treat the public REST operations as tools:

1. Search items.
2. List receiving organizations.
3. Analyze a structured shipment.
4. Present the returned exceptions.
5. Ask for confirmation.
6. Create the receipt only after confirmation.

The deterministic Spring service remains in control of validation even when an
LLM handles natural language or document interpretation.

The next real-model extension should replace only the interpretation step:

- Extract supplier lines from unstructured text or documents.
- Propose catalog candidates and structured arguments.
- Pass those arguments through the same Java validations.
- Record provider name, prompt version, confidence, latency, and reviewer action.

## 10. Interview explanation

Use this concise version:

> I built an agent-ready receiving copilot that matches sanitized supplier
> descriptions to internal inventory items, detects receipt and serial
> exceptions, and prepares an editable draft. The local provider is
> deterministic so the public project runs without credentials. I separated
> probabilistic recommendation from deterministic execution: the assistant
> cannot write inventory, a human approves the draft, Spring revalidates it, and
> PostgreSQL enforces serial uniqueness. A real Oracle AI Agent Studio agent can
> later call the same REST APIs as tools.

If asked why AI does not generate and save serials by itself:

> Serial uniqueness and inventory updates require transactional guarantees.
> AI can interpret messy supplier data and recommend a resolution, but Java and
> PostgreSQL must own the final business rule and write.
