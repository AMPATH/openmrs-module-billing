# Billing Module REST API

OpenMRS REST resources and custom endpoints exposed by `openmrs-module-billing`.

## Base URL


| Namespace      | Path prefix           |
| -------------- | --------------------- |
| Default (v1)   | `/ws/rest/v1/billing` |
| Timesheet (v2) | `/ws/rest/v2/billing` |


Standard OpenMRS REST conventions apply: JSON request/response, UUID identifiers, `v=default|full|ref` (and custom representations where supported), paging via `limit` / `startIndex` / `totalCount`.

For **create (POST)** request bodies for every resource, see [Create (POST) — all models](#create-post--all-models).

Authentication uses the normal OpenMRS session / basic auth. Privileges follow module privilege checks on each operation.

**Create convention:** Unless noted otherwise, send `POST` with `Content-Type: application/json`. Reference fields (patient, drug, payment mode, etc.) accept a **UUID string**. Successful creates return **201** with the created resource (`v=default` unless `v=` is specified). Optional client-supplied `uuid` is supported where listed.

---



## Create (POST) — all models

Quick reference for every creatable resource. Sub-resources are created under their parent path.


| Model                             | POST path                                                            |
| --------------------------------- | -------------------------------------------------------------------- |
| Bill                              | `/ws/rest/v1/billing/bill`                                           |
| Payment                           | `/ws/rest/v1/billing/bill/{billUuid}/payment`                        |
| Bill line item (append)           | `/ws/rest/v1/billing/bill/{billUuid}/lineItem`                       |
| Bill line item (nested on create) | inside `bill.lineItems`                                              |
| Bill discount                     | `/ws/rest/v1/billing/billDiscount`                                   |
| Bill refund                       | `/ws/rest/v1/billing/billRefund`                                     |
| Bill exemption                    | `/ws/rest/v1/billing/billExemption`                                  |
| Exemption rule                    | `/ws/rest/v1/billing/billExemption/{exemptionUuid}/rule`             |
| Billable service                  | `/ws/rest/v1/billing/billableService`                                |
| Billable drug                     | `/ws/rest/v1/billing/billableDrug`                                   |
| Cashier item price                | `/ws/rest/v1/billing/cashierItemPrice`                               |
| Cash point                        | `/ws/rest/v1/billing/cashPoint`                                      |
| Payment mode                      | `/ws/rest/v1/billing/paymentMode`                                    |
| Payment mode attribute type       | `/ws/rest/v1/billing/paymentModeAttributeType`                       |
| Payment attribute                 | `/ws/rest/v1/billing/paymentAttribute` *(usually nested on payment)* |
| Timesheet                         | `/ws/rest/v2/billing/timesheet`                                      |
| Billable service (legacy)         | `/ws/rest/v1/billing/api/billable-service`                           |




### Bill

```http
POST /ws/rest/v1/billing/bill
```


| Field                         | Required | Notes                                                          |
| ----------------------------- | -------- | -------------------------------------------------------------- |
| `patient`                     | Yes      | Patient UUID                                                   |
| `cashPoint`                   | No       | Cash point UUID; inferred from cashier timesheet if omitted    |
| `visit`                       | No       | Visit UUID; auto-set when patient has exactly one active visit |
| `cashier`                     | No       | Provider UUID; defaults to current provider                    |
| `status`                      | No       | Usually `PENDING`                                              |
| `lineItems`                   | No       | Array of [line item objects](#bill-line-item-nested)           |
| `payments`                    | No       | Array of payment objects (same shape as payment sub-resource)  |
| `receiptNumber`               | No       |                                                                |
| `adjustmentReason`            | No       | For adjustment bills                                           |
| `adjustedBy` / `billAdjusted` | No       | For bill adjustments                                           |
| `uuid`                        | No       | Client-supplied UUID                                           |


Do **not** nest `discounts` or `refunds` on create — use `[/billDiscount](#bill-discounts)` and `[/billRefund](#bill-refunds)`.

```json
{
  "patient": "patient-uuid",
  "cashPoint": "cash-point-uuid",
  "visit": "visit-uuid",
  "status": "PENDING",
  "lineItems": [
    {
      "quantity": 1,
      "priceUuid": "cashier-item-price-uuid"
    }
  ]
}
```



### Payment (bill sub-resource)

```http
POST /ws/rest/v1/billing/bill/{billUuid}/payment
```


| Field            | Required | Notes                                                                                                 |
| ---------------- | -------- | ----------------------------------------------------------------------------------------------------- |
| `instanceType`   | Yes      | Payment mode UUID                                                                                     |
| `amount`         | Yes      | Amount applied                                                                                        |
| `amountTendered` | Yes      | Tendered amount (≥ sum of line allocations)                                                           |
| `cashier`        | No       | Provider UUID; defaults to current provider                                                           |
| `attributes`     | No       | Payment attribute objects                                                                             |
| `lineItems`      | No       | `[{ "uuid": "<lineItemUuid>", "amount": 100 }]` — see [line-item allocations](#line-item-allocations) |


```json
{
  "instanceType": "payment-mode-uuid",
  "amount": 75,
  "amountTendered": 75,
  "lineItems": [
    { "uuid": "line-item-uuid", "amount": 75 }
  ]
}
```



### Bill line item (append)

Preferred way to add a line to an existing bill (does not re-send or rewrite existing lines):

```http
POST /ws/rest/v1/billing/bill/{billUuid}/lineItem
```


| Field           | Required | Notes                                                               |
| --------------- | -------- | ------------------------------------------------------------------- |
| `quantity`      | Yes      | Positive integer                                                    |
| `priceUuid`     | Yes      | `CashierItemPrice` UUID — amount/name/catalog snapshotted at create |
| `status`        | No       | Defaults to `PENDING`                                               |
| `lineItemOrder` | No       | Defaults to max existing order + 1                                  |
| `batchNumber`   | No       | Drug lines — Odoo batch for inventory sync                          |


```json
{
  "quantity": 1,
  "priceUuid": "cashier-item-price-uuid"
}
```

Server resolves the price row and copies `price`, `priceName`, and exactly one of `billableService` / `billableDrug` / `item` onto the line. It does **not** store a live `itemPrice` link — later catalog price edits do not change billed lines. Bill must accept new lines (`PENDING` / `POSTED`).

### Bill line item (nested on bill create)

Used inside `bill.lineItems` on create. Prefer [append](#bill-line-item-append) when adding to an existing bill.


| Field           | Required | Notes                                                            |
| --------------- | -------- | ---------------------------------------------------------------- |
| `quantity`      | Yes      | Integer                                                          |
| `priceUuid`     | Yes      | Write-only create input; snapshots amount, name, catalog owner   |
| `batchNumber`   | No       | Drug lines only                                                  |
| `status`        | No       | Defaults to `PENDING`                                            |
| `lineItemOrder` | No       | Auto-assigned via `Bill.recalculateLineItemOrder()` when omitted |


Do **not** send numeric `price`, `priceName`, or catalog UUIDs on create — those are derived from `priceUuid`.

```json
{
  "quantity": 5,
  "priceUuid": "cashier-item-price-uuid",
  "batchNumber": "BATCH-2026-01"
}
```



### Bill discount

```http
POST /ws/rest/v1/billing/billDiscount
```


| Field           | Required | Notes                                           |
| --------------- | -------- | ----------------------------------------------- |
| `bill`          | Yes      | Bill UUID                                       |
| `lineItem`      | No       | Scope discount to one line; omit for bill-level |
| `discountType`  | Yes      | `PERCENTAGE` or `FIXED_AMOUNT`                  |
| `discountValue` | Yes      | Number                                          |
| `justification` | Yes      | Reason text                                     |
| `approver`      | No       | User UUID                                       |


`initiator` is set server-side. Status defaults to `PENDING`.

```json
{
  "bill": "bill-uuid",
  "lineItem": "line-item-uuid",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "justification": "Staff discount"
}
```



### Bill refund

```http
POST /ws/rest/v1/billing/billRefund
```


| Field          | Required | Notes             |
| -------------- | -------- | ----------------- |
| `bill`         | Yes      | Bill UUID         |
| `lineItem`     | No       | Scope to one line |
| `refundAmount` | Yes      | Number            |
| `reason`       | Yes      | Text              |


`initiator` is set server-side. Status defaults to `REQUESTED`.

```json
{
  "bill": "bill-uuid",
  "refundAmount": 50,
  "reason": "Patient overpaid"
}
```



### Bill exemption

```http
POST /ws/rest/v1/billing/billExemption
```


| Field           | Required | Notes                                           |
| --------------- | -------- | ----------------------------------------------- |
| `name`          | Yes      |                                                 |
| `description`   | No       |                                                 |
| `concept`       | No       | Concept UUID                                    |
| `exemptionType` | Yes      | `SERVICE`, `COMMODITY`, or `BOTH`               |
| `rules`         | No       | Array of rule objects (or add via sub-resource) |


```json
{
  "name": "Under-5 exemption",
  "concept": "concept-uuid",
  "exemptionType": "SERVICE",
  "rules": [
    { "scriptType": "JAVASCRIPT", "script": "..." }
  ]
}
```



### Exemption rule (sub-resource)

```http
POST /ws/rest/v1/billing/billExemption/{exemptionUuid}/rule
```


| Field        | Required | Notes                                         |
| ------------ | -------- | --------------------------------------------- |
| `scriptType` | Yes      | `JAVASCRIPT`                                  |
| `script`     | Yes      | Rule script (HTML entities unescaped on save) |




### Billable service

```http
POST /ws/rest/v1/billing/billableService
```


| Field             | Required | Notes                                          |
| ----------------- | -------- | ---------------------------------------------- |
| `name`            | Yes      |                                                |
| `shortName`       | No       |                                                |
| `concept`         | Yes      | Concept UUID                                   |
| `serviceType`     | Yes      | Concept UUID                                   |
| `serviceCategory` | No       | Concept UUID                                   |
| `location`        | No       | Location UUID; omit for global catalog entry   |
| `serviceStatus`   | No       | `ENABLED` (default) or `DISABLED`              |
| `servicePrices`   | No       | Array of [price objects](#nested-price-object) |


Use `serviceStatus`, not `isDisabled` (search-only param).

```json
{
  "name": "Consultation Test",
  "shortName": "Consultation",
  "concept": "concept-uuid",
  "serviceType": "service-type-concept-uuid",
  "location": "location-uuid",
  "serviceStatus": "ENABLED",
  "servicePrices": [
    {
      "name": "Cash",
      "price": 75,
      "paymentMode": "payment-mode-uuid"
    }
  ]
}
```

**Legacy alternate** (no `location` support):

```http
POST /ws/rest/v1/billing/api/billable-service
```

Same fields except `serviceCategory` and nested prices; returns `true` on success (not the full resource).

### Billable drug

```http
POST /ws/rest/v1/billing/billableDrug
```


| Field        | Required | Notes                                          |
| ------------ | -------- | ---------------------------------------------- |
| `name`       | Yes      |                                                |
| `shortName`  | No       |                                                |
| `drug`       | Yes      | OpenMRS Drug UUID (formulation)                |
| `location`   | No       | Location UUID; omit for global                 |
| `status`     | No       | `ENABLED` (default) or `DISABLED`              |
| `drugPrices` | No       | Array of [price objects](#nested-price-object) |


REST only — no Initializer CSV domain.

```json
{
  "name": "Triomune-30",
  "shortName": "T30",
  "drug": "drug-uuid",
  "location": "location-uuid",
  "status": "ENABLED",
  "drugPrices": [
    { "name": "Cash", "price": 150, "paymentMode": "payment-mode-uuid" }
  ]
}
```



### Nested price object

Used in `servicePrices`, `drugPrices`, or standalone `cashierItemPrice`:


| Field         | Required | Notes             |
| ------------- | -------- | ----------------- |
| `name`        | Yes      | Price tier label  |
| `price`       | Yes      | Number            |
| `paymentMode` | Yes      | Payment mode UUID |


For standalone `cashierItemPrice`, set **exactly one** owner:


| Owner field       | Value                    |
| ----------------- | ------------------------ |
| `billableService` | Billable service UUID    |
| `billableDrug`    | Billable drug UUID       |
| `item`            | Stock item UUID (legacy) |




### Cashier item price

```http
POST /ws/rest/v1/billing/cashierItemPrice
```

```json
{
  "name": "Consultation - Cash",
  "price": 75,
  "paymentMode": "payment-mode-uuid",
  "billableService": "billable-service-uuid"
}
```



### Cash point

```http
POST /ws/rest/v1/billing/cashPoint
```


| Field         | Required | Notes         |
| ------------- | -------- | ------------- |
| `name`        | Yes      |               |
| `description` | No       |               |
| `location`    | Yes      | Location UUID |


```json
{
  "name": "OPD Cash Point",
  "description": "Outpatient cashier",
  "location": "location-uuid"
}
```



### Payment mode

```http
POST /ws/rest/v1/billing/paymentMode
```


| Field         | Required | Notes |
| ------------- | -------- | ----- |
| `name`        | Yes      |       |
| `description` | No       |       |


`sortOrder` and `attributeTypes` are not creatable via this resource — set `sortOrder` on update, or manage modes via Initializer `paymentmodes` CSV.

```json
{
  "name": "MPESA",
  "description": "Safaricom MPESA payment"
}
```



### Payment mode attribute type

```http
POST /ws/rest/v1/billing/paymentModeAttributeType
```


| Field            | Required | Notes                                          |
| ---------------- | -------- | ---------------------------------------------- |
| `name`           | Yes      |                                                |
| `description`    | No       |                                                |
| `format`         | No       | e.g. `java.lang.String`, `org.openmrs.Concept` |
| `foreignKey`     | No       |                                                |
| `regExp`         | No       | Validation pattern                             |
| `required`       | No       | Boolean                                        |
| `attributeOrder` | No       | Integer                                        |




### Payment attribute

Usually nested under a payment’s `attributes` array:

```json
{
  "attributeType": "attribute-type-uuid",
  "value": "0712345678"
}
```

Standalone POST to `/paymentAttribute` is supported but rarely used.

### Timesheet

```http
POST /ws/rest/v2/billing/timesheet
```


| Field       | Required | Notes                                        |
| ----------- | -------- | -------------------------------------------- |
| `cashier`   | Yes      | Provider UUID                                |
| `cashpoint` | Yes      | Cash point UUID — note lowercase `cashpoint` |
| `clockIn`   | No       | Datetime                                     |
| `clockOut`  | No       | Datetime                                     |


```json
{
  "cashier": "provider-uuid",
  "cashpoint": "cash-point-uuid",
  "clockIn": "2026-09-01T08:00:00.000+0300"
}
```

---



## Endpoint index


| Method | Path                                                     | Description                                     |
| ------ | -------------------------------------------------------- | ----------------------------------------------- |
| CRUD   | `/ws/rest/v1/billing/bill`                               | Bills                                           |
| CRUD   | `/ws/rest/v1/billing/bill/{uuid}/payment`                | Payments (sub-resource)                         |
| `POST` | `/ws/rest/v1/billing/bill/{uuid}/lineItem`               | Append one line item (`quantity` + `priceUuid`) |
| CRUD   | `/ws/rest/v1/billing/billLineItem`                       | Bill line items (prefer append / nested create) |
| CRUD   | `/ws/rest/v1/billing/billDiscount`                       | Discounts                                       |
| CRUD   | `/ws/rest/v1/billing/billRefund`                         | Refunds                                         |
| CRUD   | `/ws/rest/v1/billing/billExemption`                      | Exemptions                                      |
| CRUD   | `/ws/rest/v1/billing/billExemption/{uuid}/rule`          | Exemption rules                                 |
| CRUD   | `/ws/rest/v1/billing/billableService`                    | Billable services catalog                       |
| CRUD   | `/ws/rest/v1/billing/billableDrug`                       | Billable drugs catalog                          |
| CRUD   | `/ws/rest/v1/billing/cashierItemPrice`                   | Item / service / drug prices                    |
| CRUD   | `/ws/rest/v1/billing/cashPoint`                          | Cash points                                     |
| CRUD   | `/ws/rest/v1/billing/paymentMode`                        | Payment modes                                   |
| CRUD   | `/ws/rest/v1/billing/paymentModeAttributeType`           | Payment mode attribute types                    |
| CRUD   | `/ws/rest/v1/billing/paymentAttribute`                   | Payment attributes                              |
| CRUD   | `/ws/rest/v2/billing/timesheet`                          | Cashier timesheets                              |
| `GET`  | `/ws/rest/v1/billing/patientPaymentStatus/{patientUuid}` | Patient payment status                          |
| `GET`  | `/ws/rest/v1/billing/receipt?billUuid=`                  | Receipt PDF                                     |
| `POST` | `/ws/rest/v1/billing/api/billable-service`               | Alternate billable-service create               |


---



## Bills

**Resource:** `Bill`  
**Path:** `/ws/rest/v1/billing/bill`

### Representations


| Property              | Notes                                             |
| --------------------- | ------------------------------------------------- |
| `uuid`                |                                                   |
| `receiptNumber`       |                                                   |
| `status`              | See [BillStatus](#enums)                          |
| `dateCreated`         |                                                   |
| `patient`             | REF                                               |
| `cashier`             | REF                                               |
| `cashPoint`           | REF                                               |
| `visit`               | REF                                               |
| `lineItems`           | Nested line items                                 |
| `payments`            | FULL                                              |
| `adjustedBy`          | REF                                               |
| `billAdjusted`        | REF                                               |
| `adjustmentReason`    |                                                   |
| `discounts`           | Active discounts only (DEFAULT)                   |
| `refunds`             | Active refunds only; empty without `VIEW_REFUNDS` |
| `total`               | Read-only                                         |
| `amountAfterDiscount` | Read-only                                         |




### Creatable properties

`patient`, `cashier`, `cashPoint`, `visit`, `lineItems`, `payments`, `receiptNumber`, `status`, `adjustmentReason`, `adjustedBy`, `billAdjusted`, `uuid`

Do **not** create discounts nested on the bill — use `[/billDiscount](#bill-discounts)`.

### Search parameters


| Param            | Description                                                                  |
| ---------------- | ---------------------------------------------------------------------------- |
| `patientUuid`    | Filter by patient                                                            |
| `patientName`    | Filter by patient name                                                       |
| `status`         | Comma-separated [BillStatus](#enums) values                                  |
| `cashPointUuid`  | Filter by cash point                                                         |
| `visitUuid`      | Filter by visit                                                              |
| `locationUuid`   | Filter by cash point location                                                |
| `discountStatus` | Comma-separated `PENDING` / `APPROVED` / `REJECTED`                          |
| `refundStatus`   | Requires `VIEW_REFUNDS`; `REQUESTED` / `APPROVED` / `REJECTED` / `COMPLETED` |
| `includeAll`     | When `true`, include voided line items                                       |




### Examples

```http
GET /ws/rest/v1/billing/bill?patientUuid={uuid}&status=PENDING,POSTED&locationUuid={locationUuid}
```

```http
POST /ws/rest/v1/billing/bill
Content-Type: application/json

{
  "patient": "patient-uuid",
  "cashPoint": "cash-point-uuid",
  "visit": "visit-uuid",
  "status": "PENDING",
  "lineItems": [
    {
      "quantity": 1,
      "priceUuid": "cashier-item-price-uuid"
    }
  ]
}
```



### Notes

- On create, cashier defaults to the current provider when omitted; cash point may be inferred from the cashier timesheet.
- If the patient has exactly one active visit and `visit` is omitted, that visit may be assigned automatically.
- Status transitions via REST are limited (e.g. `null → any`, `PENDING → POSTED`). Posting can trigger rounding line items.
- Void uses standard DELETE (void); purge is supported.
- Same patient + visit orders are consolidated onto one open (`PENDING` / `POSTED`) bill by order billing strategies.

---



## Payments (bill sub-resource)

**Path:** `/ws/rest/v1/billing/bill/{billUuid}/payment`

### Representations


| Property         | Notes                             |
| ---------------- | --------------------------------- |
| `uuid`           |                                   |
| `instanceType`   | Payment mode (REF)                |
| `attributes`     | Payment attributes                |
| `amount`         |                                   |
| `amountTendered` |                                   |
| `cashier`        | REF                               |
| `dateCreated`    |                                   |
| `voided`         |                                   |
| `lineItems`      | Line-item allocations (see below) |




### Creatable properties

`instanceType`, `attributes`, `amount`, `amountTendered`, `cashier`, `lineItems`

### Line-item allocations

Allocate a payment to specific bill line items without requiring the whole bill to be paid.

**Write / read shape:**

```json
"lineItems": [
  { "uuid": "<billLineItemUuid>", "amount": 100.00 }
]
```

**Rules:**

- Each entry requires `uuid` (bill line item) and a positive `amount`.
- The line item must belong to the parent bill.
- Cannot allocate to lines with status `PAID` or `EXEMPTED`.
- Sum of allocation amounts must not exceed `amountTendered`.
- Voided allocations are omitted from the representation.
- Bill becomes `PAID` only when all payable line items are paid; otherwise it stays `POSTED` when any unpaid lines remain.



### Example

```http
POST /ws/rest/v1/billing/bill/{billUuid}/payment
Content-Type: application/json

{
  "instanceType": "payment-mode-uuid",
  "amount": 100.00,
  "amountTendered": 100.00,
  "lineItems": [
    { "uuid": "line-item-uuid-1", "amount": 100.00 }
  ]
}
```



### Notes

- Cashier defaults to the current provider when omitted.
- DELETE voids the payment and re-synchronizes bill status.
- Purge removes the payment from the bill.
- Payment UUID alone is not enough for lookup — always use the parent bill path.

---



## Bill line items

**Path:** `/ws/rest/v1/billing/billLineItem`

**Create:** Prefer [append](#bill-line-item-append) `POST /bill/{uuid}/lineItem` or nested [bill create](#bill-line-item-nested-on-bill-create) with `quantity` + `priceUuid`. Standalone POST is not fully supported (`getServiceClass` is null).

Price amount and name are **snapshotted** from `CashierItemPrice` at create time (no live price FK). Catalog price changes do not rewrite existing lines.

### Representations


| Property                                  | Notes                                                |
| ----------------------------------------- | ---------------------------------------------------- |
| `uuid`, `display`, `voided`, `voidReason` | Base                                                 |
| `item`                                    | Stock item (legacy); getter returns drug name string |
| `billableService`                         | Service catalog ref                                  |
| `billableDrug`                            | Drug catalog ref                                     |
| `quantity`                                |                                                      |
| `price`                                   |                                                      |
| `priceName`                               |                                                      |
| `lineItemOrder`                           |                                                      |
| `batchNumber`                             | Optional; for drug inventory sync (e.g. Odoo)        |
| `status`                                  | See [BillLineItemStatus](#enums)                     |
| `auditInfo`                               | Full representation                                  |




### Notes

- Prefer [append](#bill-line-item-append) when adding a line to an existing bill — avoid full `lineItems` replace on bill update.
- Create with `priceUuid` only; response includes denormalized `price` / `priceName` / catalog refs.
- `batchNumber` is for drug lines: set by the frontend (batch dropdown); billing stores it for inventory sync.
- Void requires a reason and saves the parent bill.



### Example (append drug line with batch)

```http
POST /ws/rest/v1/billing/bill/{billUuid}/lineItem
Content-Type: application/json

{
  "quantity": 5,
  "priceUuid": "cashier-item-price-uuid",
  "batchNumber": "BATCH-2026-01"
}
```

---



## Bill discounts

**Path:** `/ws/rest/v1/billing/billDiscount`

**Create:** See [Bill discount](#bill-discount).

### Representations


| Rep         | Properties                                                                                             |
| ----------- | ------------------------------------------------------------------------------------------------------ |
| **ref**     | `uuid`, `discountType`, `discountAmount`, `status`, `voided`                                           |
| **default** | + `billUuid`, `lineItemUuid`, `discountValue`, `justification`, `initiator`, `approver`, `dateCreated` |
| **full**    | initiator/approver in DEFAULT + `auditInfo`                                                            |




### Creatable / updatable


| Operation | Properties                                                                       |
| --------- | -------------------------------------------------------------------------------- |
| Create    | `bill`, `lineItem`, `discountType`, `discountValue`, `justification`, `approver` |
| Update    | `approver`, `status`                                                             |




### Search


| Param  | Required | Description                                                      |
| ------ | -------- | ---------------------------------------------------------------- |
| `bill` | Yes      | Bill UUID (returns all discounts for the bill, including voided) |


**Enums:** `DiscountType` = `PERCENTAGE`  `FIXED_AMOUNT`; `DiscountStatus` = `PENDING`  `APPROVED`  `REJECTED`

Initiator is set automatically. Purge is not supported; void is supported.

---



## Bill refunds

**Path:** `/ws/rest/v1/billing/billRefund`

**Create:** See [Bill refund](#bill-refund).

### Representations


| Rep         | Properties                                                                                                                   |
| ----------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **ref**     | `uuid`, `refundAmount`, `status`, `voided`                                                                                   |
| **default** | + `billUuid`, `lineItemUuid`, `reason`, `initiator`, `approver`, `completer`, `dateApproved`, `dateCompleted`, `dateCreated` |
| **full**    | users as DEFAULT + `auditInfo`                                                                                               |




### Creatable / updatable


| Operation | Properties                                   |
| --------- | -------------------------------------------- |
| Create    | `bill`, `lineItem`, `refundAmount`, `reason` |
| Update    | `status`, `approver`, `completer`            |




### Search


| Param  | Required | Description |
| ------ | -------- | ----------- |
| `bill` | Yes      | Bill UUID   |


**RefundStatus:** `REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED`

Purge is not supported; void is supported.

---



## Bill exemptions

**Path:** `/ws/rest/v1/billing/billExemption`

**Create:** See [Bill exemption](#bill-exemption) and [Exemption rule](#exemption-rule-sub-resource).

### Representations


| Rep         | Properties                                            |
| ----------- | ----------------------------------------------------- |
| **ref**     | `uuid`, `name`, `description`, `retired`              |
| **default** | + `retireReason`, `concept`, `exemptionType`, `rules` |
| **full**    | concept DEFAULT, rules FULL, `auditInfo`              |




### Creatable / updatable

`name`, `description`, `concept`, `exemptionType`, `rules`

**ExemptionType:** `SERVICE`, `COMMODITY`, `BOTH`

DELETE retires; purge is not supported.

### Exemption rules (sub-resource)

**Path:** `/ws/rest/v1/billing/billExemption/{uuid}/rule`


| Property                | Notes        |
| ----------------------- | ------------ |
| `uuid`                  |              |
| `scriptType`            | `JAVASCRIPT` |
| `script`                | Rule script  |
| `voided` / `voidReason` |              |
| `auditInfo`             | Full         |


Creatable/updatable: `scriptType`, `script`.

---



## Billable services

**Path:** `/ws/rest/v1/billing/billableService`

Catalog of concept-based billable services (labs, procedures, etc.).

### Representations

`name`, `shortName`, `concept`, `serviceType`, `serviceCategory`, `location` (REF), `servicePrices`, `serviceStatus`

### Creatable / updatable

Same as the default representation.

### Search parameters


| Param                        | Description                                             |
| ---------------------------- | ------------------------------------------------------- |
| `serviceType`                | Concept UUID                                            |
| `serviceCategory`            | Concept UUID                                            |
| `isDisabled`                 | `yes` / `1` → `DISABLED`; otherwise `ENABLED`           |
| `serviceName`                | Name contains filter                                    |
| `location` or `locationUuid` | Location filter; excludes global (`location` null) rows |


**Status:** `ENABLED`  `DISABLED`

### Example

```http
POST /ws/rest/v1/billing/billableService
Content-Type: application/json

{
  "name": "Complete Blood Count",
  "shortName": "CBC",
  "concept": "concept-uuid",
  "serviceType": "service-type-concept-uuid",
  "serviceCategory": "category-concept-uuid",
  "location": "location-uuid",
  "serviceStatus": "ENABLED",
  "servicePrices": [
    {
      "name": "Standard",
      "price": 75.00,
      "paymentMode": "payment-mode-uuid"
    }
  ]
}
```



### Alternate create endpoint

```http
POST /ws/rest/v1/billing/api/billable-service
```

Body fields: `name`, `shortName`, `concept`, `serviceType`, `serviceCategory`, `servicePrices[]` (`name`, `price`, `paymentMode`), `serviceStatus` (default `ENABLED`). This path does not set `location`.

---



## Billable drugs

**Path:** `/ws/rest/v1/billing/billableDrug`

Catalog of formulation-level (`org.openmrs.Drug`) billable drugs. Managed via REST only (no Initializer). Prefer BillableDrug over StockItem when pricing drug orders.

### Representations

`name`, `shortName`, `drug` (REF), `location` (REF), `drugPrices`, `status`

### Creatable / updatable

`name`, `shortName`, `drug`, `location`, `drugPrices`, `status`

`drug` and `location` accept UUIDs. Nested `drugPrices` are `CashierItemPrice` rows (multiple prices / payment modes supported).

### Search parameters


| Param                        | Description                                                 |
| ---------------------------- | ----------------------------------------------------------- |
| `drug` or `drugUuid`         | OpenMRS Drug UUID                                           |
| `location` or `locationUuid` | Location filter; excludes global (`location` null) rows |
| `name`                       | Name contains filter                                        |
| `status`                     | `ENABLED` / `DISABLED` (defaults to `ENABLED` when omitted) |




### Example

```http
POST /ws/rest/v1/billing/billableDrug
Content-Type: application/json

{
  "name": "Triomune-30",
  "shortName": "T30",
  "drug": "drug-uuid",
  "location": "location-uuid",
  "status": "ENABLED",
  "drugPrices": [
    {
      "name": "Cash",
      "price": 150.00,
      "paymentMode": "payment-mode-uuid"
    }
  ]
}
```

```http
GET /ws/rest/v1/billing/billableDrug?drugUuid={drugUuid}&locationUuid={locationUuid}
```

Purge is supported.

---



## Cashier item prices

**Path:** `/ws/rest/v1/billing/cashierItemPrice`

**Create:** See [Cashier item price](#cashier-item-price) and [Nested price object](#nested-price-object).

Shared price rows for stock items, billable services, or billable drugs (exactly one owner).

### Representations / creatable

`name`, `price`, `paymentMode`, `item`, `billableService`, `billableDrug`

`GET` with `includeAll=true` includes retired prices. DELETE retires; undelete and purge are supported.

---



## Cash points

**Path:** `/ws/rest/v1/billing/cashPoint`

**Create:** See [Cash point](#cash-point).

### Representations


| Rep         | Properties                               |
| ----------- | ---------------------------------------- |
| **ref**     | `uuid`, `name`, `description`, `retired` |
| **default** | + `location` (REF)                       |
| **full**    | + `auditInfo`                            |


Creatable includes metadata fields plus `location`. `includeAll=true` includes retired. DELETE retires; purge supported.

---



## Payment modes

**Path:** `/ws/rest/v1/billing/paymentMode`

**Create:** See [Payment mode](#payment-mode), [Payment mode attribute type](#payment-mode-attribute-type), and [Payment attribute](#payment-attribute).

### Representations

`uuid`, `name`, `description`, `retired`, `retireReason`, `sortOrder`, `attributeTypes` (REF)

`includeAll=true` includes retired. Purge supported.

### Payment mode attribute types

**Path:** `/ws/rest/v1/billing/paymentModeAttributeType`

Properties include metadata fields plus `attributeOrder`, `format`, `foreignKey`, `regExp`, `required`.

### Payment attributes

**Path:** `/ws/rest/v1/billing/paymentAttribute`

Usually nested under a payment’s `attributes`. Properties: `value`, `attributeType`, `order`, `valueName`.

---



## Timesheets

**Path:** `/ws/rest/v2/billing/timesheet`

**Create:** See [Timesheet](#timesheet).

### Representations

`cashier` (REF), `cashPoint` (REF), `clockIn`, `clockOut`, plus base data properties.

### Creatable

Base creatable properties plus `cashier` and `cashpoint` (note the lowercase spelling of this property key).

### Search


| Param  | Required | Description                                                         |
| ------ | -------- | ------------------------------------------------------------------- |
| `date` | Yes      | `MM/dd/yyyy` — timesheets for the **current provider** on that date |


```http
GET /ws/rest/v2/billing/timesheet?date=08/25/2026
```

---



## Custom endpoints



### Patient payment status

```http
GET /ws/rest/v1/billing/patientPaymentStatus/{patientUuid}
```

**Response:** `{ "status": "PAID|UNPAID|UNKNOWN", "reason": "..." }`  
**404** if the patient UUID is not found.

### Receipt PDF

```http
GET /ws/rest/v1/billing/receipt?billUuid={uuid}
```

**Produces:** `application/pdf`  
**Headers:** `Content-Disposition: inline; filename="receipt-{id}.pdf"`  
**404** if bill missing; **204** if an empty PDF is generated.

---



## Enums


| Enum                                           | Values                                                                                                                 |
| ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **BillStatus**                                 | `PENDING`, `POSTED`, `PAID`, `CANCELLED`, `ADJUSTED`, `EXEMPTED`, `REFUND_REQUESTED`, `REFUNDED`, `PARTIALLY_REFUNDED` |
| **BillLineItemStatus**                         | `PENDING`, `PAID`, `CANCELLED`, `ADJUSTED`, `EXEMPTED`, `REFUND_REQUESTED`, `REFUNDED`, `PARTIALLY_REFUNDED`           |
| **DiscountType**                               | `PERCENTAGE`, `FIXED_AMOUNT`                                                                                           |
| **DiscountStatus**                             | `PENDING`, `APPROVED`, `REJECTED`                                                                                      |
| **RefundStatus**                               | `REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED`                                                                       |
| **ExemptionType**                              | `SERVICE`, `COMMODITY`, `BOTH`                                                                                         |
| **ScriptType**                                 | `JAVASCRIPT`                                                                                                           |
| **BillableServiceStatus / BillableDrugStatus** | `ENABLED`, `DISABLED`                                                                                                  |
| **PatientPaymentStatus**                       | `PAID`, `UNPAID`, `UNKNOWN`                                                                                            |


---



## Common workflows

### Auto-bill on order create

Global property `billing.autoBillOnOrderCreate` (default `false`) controls whether Order CREATED events automatically create/update bills.

- `false` (default): no automatic bills; create bills/line items via REST
- `true`: order strategies create/append bill line items as before

To void (retire) a line item:

```http
DELETE /ws/rest/v1/billing/billLineItem/{lineItemUuid}?reason=Removed+in+error
```

### 1. Create a location-scoped billable drug and price it

```http
POST /ws/rest/v1/billing/billableDrug
→ { "name", "drug", "location", "status": "ENABLED", "drugPrices": [...] }
```

Drug orders resolve BillableDrug by drug UUID + location (with global fallback), then StockItem if none exists.

### 2. Pay one line item on a posted bill

```http
POST /ws/rest/v1/billing/bill/{billUuid}/payment
→ {
    "instanceType": "...",
    "amount": 75,
    "amountTendered": 75,
    "lineItems": [{ "uuid": "line-uuid", "amount": 75 }]
  }
```

That line becomes `PAID`; the bill stays `POSTED` until every payable line is paid.

### 3. Set batch number for inventory sync

Update the drug line item (or include on create) with `batchNumber` from the Odoo batch dropdown. Billing only stores the value; it does not fetch batches from Odoo.

### 4. Search bills for a facility

```http
GET /ws/rest/v1/billing/bill?locationUuid={facilityLocationUuid}&status=PENDING,POSTED
```

---



## Related docs

- [Billing multi-feature plan](./billing-multi-feature-plan.md) — design decisions for location scoping, BillableDrug, line-item payments, and visit consolidation.

