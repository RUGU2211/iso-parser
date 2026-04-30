# ISO Parser - Current Implementation Flow (Updated)

## 1) Current Project Purpose

This service processes card-limit update messages and supports dynamic limit onboarding through DB config.  
It can receive request payloads as:

- ISO8583 HEX message (primary path)
- raw XML payload
- text payload that contains embedded XML block

It then:

1. extracts all limit fields dynamically,
2. resolves known fields from `limit_master`,
3. generates TLV-like limit string dynamically,
4. stores known/unknown fields in separate tables,
5. writes audit logs for every request.

## 2) Current API Surface (As Implemented)

Single route family is used:

- `GET /api/iso/limit/{pan}`
- `POST /api/iso/limit/{pan}`
- `PUT /api/iso/limit/{pan}`
- `DELETE /api/iso/limit/{pan}`

Content-type behavior:

- `POST /api/iso/limit/{pan}` with `text/plain` -> parsing flow (`IsoController`)
- `POST /api/iso/limit/{pan}` with `application/json` -> CRUD create flow (`CardLimitCrudController`)
- `PUT /api/iso/limit/{pan}` with `application/json` -> CRUD upsert flow

This allows one endpoint family while separating parse and CRUD behavior using content type.

## 3) End-to-End Parse Flow (text/plain)

1. Request enters `IsoController`.
2. Controller passes input string to `IsoMessageService.processIsoMessage(...)`.
3. Service creates `IsoAudit` skeleton (`req_in`, `binary_hex`, `request_id`, timestamp).
4. Service chooses parse strategy:
   - raw XML input -> parse directly
   - text containing XML block -> extract XML substring and parse
   - otherwise -> parse as HEX ISO via jPOS parser
5. `XmlLimitParser` maps XML into `CardLimitDTO`:
   - validates PAN/ExpiryDate
   - defaults `SeqNr` if missing
   - collects all `<Field Name="...">value</Field>` into dynamic map
6. `LimitCalculationService`:
   - loads active master rows sorted by priority
   - splits known vs unknown fields
   - generates one segment per known field (dynamic lengths)
   - concatenates segments in priority order to final payload
7. Service writes:
   - summary record in `card_limits`
   - known structured records in `card_limit_values`
   - unknown records in `limit_extra_data`
8. Service finalizes `iso_audit` with response, DE39, duration, and error (if any).
9. Returns JSON response:
   - success -> `de39=00`
   - failure -> `de39=01`

## 4) CRUD Flow (application/json)

## `POST /api/iso/limit/{pan}` (create)
- Creates record only if PAN not already present.
- Returns `409` if PAN already exists.

## `PUT /api/iso/limit/{pan}` (upsert)
- If PAN exists -> updates record.
- If PAN not found -> creates new record.

## `GET /api/iso/limit/{pan}`
- Returns array of record views for PAN, including:
  - summary payload,
  - known limits map,
  - unknown limits map.

## `DELETE /api/iso/limit/{pan}`
- Deletes PAN data from:
  - `card_limits`
  - `card_limit_values`
  - `limit_extra_data`

## 5) Layer-by-Layer Implementation

## Controller Layer

### `IsoController`
- Handles parse-mode `text/plain` requests on `/api/iso/limit/{pan}`.
- Converts service result to HTTP response based on success and DE39.

### `CardLimitCrudController`
- Handles JSON CRUD operations on same route family.
- Uses explicit `consumes = application/json` on POST/PUT to avoid overlap.

## Parser Layer

### `Iso8583MessageParser`
- Validates HEX.
- Tries unpack using `PostPackager`, then fallback `ISO87APackager`.
- Extracts XML from `127.22` / `127.022` / `127`.
- Extracts `DE11`.

### `XmlLimitParser`
- Cleans prefix/noise before XML.
- Supports `InquiryOrUpdateData` and `Postilion:InquiryOrUpdateData`.
- Validates required card fields.
- Dynamically captures all limit fields.
- Enforces numeric values for limit field content.

## Service Layer

### `IsoMessageService`
- Transactional orchestration.
- Flexible input mode (HEX/XML/text-with-XML).
- Persists summary, structured known/unknown, and audit records.

### `CardLimitCrudService`
- Implements PAN-only create/upsert/get/delete behavior.
- Reuses dynamic calculation service for known/unknown split and TLV generation.

### `LimitCalculationService`
- Reads `limit_master` active rows ordered by `priority`.
- Known fields -> build segment with profile/rule from DB.
- Unknown fields -> returned separately.
- Produces final `limitPayload` by ordered concatenation.

## 6) Database Model (Current)

## `limit_master`
Control table for known-limit behavior.

Columns used by engine:
- `limit_name`
- `limit_pnr`
- `limit_rule_nr`
- `priority`
- `is_active`
- optional metadata: `limit_type`

## `card_limits`
Summary record table.

Stores:
- PAN/Seq/ISO identifiers
- final generated `limits` payload
- update metadata

## `card_limit_values`
Structured known-limit store.

Stores one row per known field:
- `field_name`, `field_value`
- profile/rule/priority used

## `limit_extra_data`
Unknown-field capture table.

Stores fields not currently active in master:
- `field_name`, `field_value`, source

## `iso_audit`
Audit and observability.

Stores:
- request and response snapshots
- parsed attributes
- DE39, DE11
- `request_id`, `processing_time_ms`, `error_message`

## 7) Known vs Unknown Resolution Rule

- Known: `limit_name` exists in active `limit_master`.
  - goes to `card_limit_values`
  - included in final `limits` payload
- Unknown: no active master row found.
  - goes to `limit_extra_data`
  - not included in final payload

This is dynamic and does not require code changes when master rows are added.

## 8) Priority Behavior (Critical)

`priority` controls segment sequence in final `limits` payload.

- lower priority value -> earlier segment
- higher priority value -> later segment

It does not change amount/value logic.  
It only changes ordering among known active fields.

## 9) Dynamic Extension Without Code Change

To support a new known field (example: `atm_limit`):

1. Insert row in `limit_master` with:
   - `limit_name='atm_limit'`
   - `limit_pnr`, `limit_rule_nr`
   - `priority`
   - `is_active=true`
2. Send field in incoming XML.
3. Engine auto-recognizes it as known and includes in payload/table.

If `is_active=false`, same field is stored as unknown in `limit_extra_data`.

## 10) Input Format Notes (Current Reality)

- Manually editing XML inside an old HEX message may fail because ISO field lengths become invalid.
- Correct HEX should be regenerated using packager logic.
- Root helper file available:
  - `IsoHexGenerator.java`
  - used to generate valid packed HEX from updated XML content.

## 11) Operational / Testing Notes

- Postman collection uses `/api/iso/limit/{pan}` route family.
- Main tests are HEX parsing scenarios; CRUD section is optional admin/testing.
- Recommended DB checks after tests:
  - `card_limits`
  - `card_limit_values`
  - `limit_extra_data`
  - `iso_audit`

## 12) Current Status Summary

- Config-driven known/unknown resolution: implemented.
- Priority-ordered dynamic segment generation: implemented.
- PAN-only CRUD route family: implemented.
- Flexible parse input modes (HEX/XML/text-with-XML): implemented.
- Root ISO HEX generator helper: available.
