# ISO8583 Limit Parser Service

[![Java 17](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue.svg)](https://www.postgresql.org/)

Spring Boot API that parses ISO8583 HEX messages, extracts XML from Field 127.022 (Postilion), parses card limits, applies master table rules, and persists to PostgreSQL.

**Repository:** [https://github.com/RUGU2211/iso-parser](https://github.com/RUGU2211/iso-parser)

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| API | Spring Boot |
| Parsing | jPOS |
| XML Parsing | Jackson |
| ORM | JPA + Hibernate |
| Database | PostgreSQL |

---

## Project Working (In Detail)

### 1. Overview

This service simulates a **bank card limit update system** used in ATM switches, POS switches, and payment networks. It receives ISO8583 messages (typically from systems like Postilion, Visa BASE24, Mastercard), extracts card limit data from XML embedded in Field 127.022, converts limits using a master table, and stores the result in PostgreSQL.

### 2. End-to-End Flow

```
┌─────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  POST /api/iso  │     │  IsoController   │     │ IsoMessageService│
│  /parse        │────▶│  (raw hex/JSON)   │────▶│  (orchestrator)  │
└─────────────────┘     └──────────────────┘     └────────┬─────────┘
                                                           │
                                                           ▼
┌─────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   PostgreSQL    │◀────│ CardLimitRepository│◀────│  CardLimit entity │
│  card_limits    │     │  (upsert by PAN)  │     │  (limits string)  │
└─────────────────┘     └──────────────────┘     └────────┬─────────┘
                                                           │
                                                           ▼
┌─────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ Iso8583Parser   │────▶│  XmlLimitParser  │────▶│LimitCalculation  │
│ (extract XML)   │     │  (parse to DTO)  │     │Service (build     │
│                 │     │                  │     │ limit string)     │
└─────────────────┘     └──────────────────┘     └──────────────────┘
```

### 3. Step-by-Step Processing

#### Step 1: API Request

- **Endpoint:** `POST /api/iso/parse`
- **Input formats:**
  - **Raw HEX:** `Content-Type: text/plain`, body = hex string (e.g. `30363030e23c4680...`)
  - **JSON:** `Content-Type: application/json`, body = `{"isoMessage": "30363030e23c4680..."}`

#### Step 2: ISO8583 Parsing (Iso8583MessageParser)

1. **Validation:** HEX string is validated (non-empty, even length, valid chars 0-9A-Fa-f).
2. **Conversion:** HEX string → byte array via `HexUtil.hexToBytes()`.
3. **Unpack:** Uses jPOS (PostPackager or ISO87APackager) to unpack the ISO message.
4. **Extract:** Reads Field 127 (or 127.22 / 127.022 for Postilion format).
5. **Output:** Raw XML string from the field.

#### Step 3: XML Parsing (XmlLimitParser)

1. **Extract:** Finds `<InquiryOrUpdateData>` or `<Postilion:InquiryOrUpdateData>` in the XML.
2. **Parse:** Jackson deserializes into `InquiryOrUpdateData` → `Card` → `LimitField` list.
3. **Map:** Extracts:
   - `PAN` (card number)
   - `ExpiryDate`
   - `SeqNr` (card sequence)
   - `cash_limit` (ATM limit)
   - `goods_limit` (POS limit)
   - `card_not_present_limit` (E-commerce limit)
4. **Output:** `CardLimitDTO` with all parsed values.

#### Step 4: Limit String Calculation (LimitCalculationService)

1. **Load rules:** Reads `limit_master` table (goods_limit, card_not_present_limit, cash_limit).
2. **Build format:** If `limit_master` has data, builds:
   ```
   prefix1 + 21nines | pos_limit | 21nines | cash_limit 12nines + prefix2 + 21nines | ecom_limit + 21nines + cash_limit + 12nines
   ```
3. **Fallback:** If `limit_master` is empty, uses simple `cash|goods|ecom` format.
4. **Output:** Formatted limit string.

#### Step 5: Database Persistence (IsoMessageService)

1. **Upsert:** Looks up existing record by `pan` + `seqNr`.
2. **Update or insert:** If found → update; if not → create new.
3. **Save:** Sets iso_nr, pan, seq_nr, limits, last_upd_date, last_upd_user.
4. **Persist:** `cardLimitRepository.save(entity)`.

### 4. Input Message Structure

**ISO8583 Field 127.022** contains XML like:

```xml
<InquiryOrUpdateData>
  <Card>
    <PAN>3538210000000026</PAN>
    <ExpiryDate>3005</ExpiryDate>
    <SeqNr>001</SeqNr>
    <Field Name="cash_limit">20000</Field>
    <Field Name="goods_limit">80000</Field>
    <Field Name="card_not_present_limit">90000</Field>
  </Card>
</InquiryOrUpdateData>
```

| XML Field | Meaning |
|-----------|---------|
| PAN | Card number |
| ExpiryDate | Card expiry (MMYY) |
| SeqNr | Card sequence |
| cash_limit | ATM withdrawal limit |
| goods_limit | POS purchase limit |
| card_not_present_limit | E-commerce limit |

### 5. Limit String Format

**Example output for PAN=3538210000000026, cash=20000, goods=80000, ecom=90000:**

```
124229212522810999999999999999999999999|80000|999999999999999999999999|20000 9999999999991241292125328510999999999999999999999999|90000999999999999999999999200009999999999999
```

- **Format:** `prefix1 + 21nines | pos | 21nines | cash 12nines + prefix2 + 21nines | ecom + 21nines + cash + 12nines`
- **pos** = goods_limit (POS)
- **ecom** = card_not_present_limit (ECOM)
- **cash** = cash_limit (ATM)

### 6. Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| **IsoController** | Accepts request, validates content type, calls service, returns success/error response |
| **IsoMessageService** | Orchestrates parsing and persistence |
| **Iso8583MessageParser** | Extracts XML from ISO8583 Field 127/127.022 |
| **XmlLimitParser** | Parses XML into CardLimitDTO |
| **LimitCalculationService** | Builds limit string from DTO using limit_master |
| **CardLimitRepository** | JPA: findByPanAndSeqNr, save |
| **LimitMasterRepository** | JPA: loads limit rules |
| **GlobalExceptionHandler** | Handles IsoParseException, XmlParseException, etc. |

### 7. Error Handling

| Exception | HTTP Status | Response |
|-----------|--------------|----------|
| IsoParseException | 400 | `{"success": false, "message": "ISO parse failed: ..."}` |
| XmlParseException | 400 | `{"success": false, "message": "XML parse failed: ..."}` |
| Other Exception | 500 | `{"success": false, "message": "Error: ..."}` |

---

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL (create database `isodb`)
- Maven

### Clone & Run

```bash
git clone https://github.com/RUGU2211/iso-parser.git
cd iso-parser
mvn spring-boot:run
```

The API runs at **http://localhost:8081**

### API

**POST** `http://localhost:8081/api/iso/parse`

**Option 1 – Raw HEX (Content-Type: text/plain):**
```
30363030e23c468000e00000000000001000002231363335333231303030303030303032363931303030303033313331313237343932353230333531313237343930333133333030353131313131303030303031313630313233343536373132333435363738393031323334356162636465666768696a6b6c6d6e6f707172737475767778797a3031323334353637383930313233303531303031313031353135333232303337323733373330303030303336344000040080000000313432303236303331333131323734393030333331323239506f7374696c696f6e3a496e71756972794f7255706461746544617461333239353c3f786d6c2076657273696f6e3d22312e302220656e636f64696e673d225554462d38223f3e3c496e71756972794f72557064617465446174613e3c436172643e3c50414e3e333533383231303030303030303032363c2f50414e3e3c457870697279446174653e333030353c2f457870697279446174653e3c5365714e723e3030313c2f5365714e723e3c4669656c64204e616d653d22636173685f6c696d6974223e32303030303c2f4669656c643e3c4669656c64204e616d653d22676f6f64735f6c696d6974223e38303030303c2f4669656c643e3c4669656c64204e616d653d22636172645f6e6f745f70726573656e745f6c696d6974223e39303030303c2f4669656c643e3c2f436172643e3c2f496e71756972794f72557064617465446174613e35303236
```

**Option 2 – JSON (Content-Type: application/json):**
```json
{
  "isoMessage": "30363030e23c4680..."
}
```

**Success (200):**
```json
{
  "success": true,
  "message": "ISO message processed successfully"
}
```

**Error (400/500):**
```json
{
  "success": false,
  "message": "ISO parse failed: Invalid HEX format"
}
```

---

## Database

### Tables

- **card_limits:** Parsed card limit data (pan, seq_nr, limits, last_upd_date, etc.)
- **limit_master:** Rule mapping for limit conversion

### Limit Master Seed Data

| limit_name | limit_pnr | limit_rule_nr |
|------------|-----------|---------------|
| goods_limit | 42 | 52 (POS) |
| card_not_present_limit | 43 | 53 (ECOM) |
| cash_limit | 44 | 54 (CASH) |

### Unique Key & Upsert (Card Processing Standard)

In card processing systems (Postilion / ISO8583), **PAN + SeqNr** is the unique key:

- **Unique constraint:** `UNIQUE(pan, seq_nr)` on `card_limits`
- **Upsert:** If record exists for same PAN + SeqNr → **update**; otherwise → **insert**
- **Lookup:** `findByPanAndSeqNr(pan, seqNr)` before save

---

## Configuration

- **application.yml:** Port 8081, PostgreSQL, Hibernate ddl-auto, data.sql for limit_master seed
- **data.sql:** Seeds limit_master on startup

### Configurable Properties (no hardcoded data)

All runtime values are configurable via `application.yml`:

| Property | Description | Default |
|----------|-------------|---------|
| `iso.default-nr` | Default ISO number | 13 |
| `iso.default-seq-nr` | Default sequence when missing | 001 |
| `iso.last-upd-user` | Last update user | sp |
| `limit.max-21` | Max value (21 nines) for limit format | 999999999999999999999999 |
| `limit.max-12` | Max value (12 nines) | 999999999999 |
| `limit.prefix-1` | Limit string prefix 1 | 124229212522810 |
| `limit.prefix-2` | Limit string prefix 2 | 1241292125328510 |
| `limit.default-value` | Default when limit missing | 0 |

---

## Project Structure

```
iso-parser/
├── config/         IsoConfig, LimitConfig
├── controller/     IsoController
├── service/        IsoMessageService, LimitCalculationService
├── parser/         Iso8583MessageParser, XmlLimitParser
├── parser/xml/     InquiryOrUpdateData, Card, LimitField (Jackson XML)
├── repository/     CardLimitRepository, LimitMasterRepository
├── entity/         CardLimit, LimitMaster
├── dto/            CardLimitDTO, IsoParseRequest, IsoParseResponse
├── exception/      IsoParseException, XmlParseException, GlobalExceptionHandler
└── util/           HexUtil
```

---

## Testing

See **POSTMAN_TEST.md** for Postman setup and sample HEX payload.

---

## Real-World Context

This project simulates bank switch systems where:

1. **ATM/POS** sends ISO8583 messages to a payment switch.
2. **Switch (Postilion, etc.)** forwards messages with card limit updates.
3. **This service** parses the message and updates card limits in the database.
4. **Database** stores the formatted limit string for downstream systems.
