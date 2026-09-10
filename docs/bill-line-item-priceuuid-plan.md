# Bill line items via priceUuid (create-time snapshot)

**Overview:** Add `POST /bill/{uuid}/lineItem` that appends a line from `quantity` + `priceUuid`, snapshotting price/name/catalog at create time (no live CashierItemPrice dependency). Nested bill create uses the same snapshot rule instead of a client-supplied numeric `price`.

## Todos

- [x] Add helper that snapshots CashierItemPrice onto BillLineItem at create (no itemPrice FK)
- [x] Fix BillLineItemResource priceUuid as write-only create input; snapshot fields; drop raw price from creatable
- [x] Add POST /bill/{uuid}/lineItem controller with quantity + priceUuid
- [x] Tests for snapshot-on-create, append, and create-via-priceUuid; catalog price change does not affect line
- [x] Update docs/rest-api.md create sections (priceUuid write-only; denormalized price)

## Problem

Frontend today does:

```js
await updateBill(currentBill.uuid, {
  lineItems: [...initialLineItems, ...newLineItems],
});
```

[`BillResource.setBillLineItems`](../omod/src/main/java/org/openmrs/module/billing/web/rest/resource/BillResource.java) uses `syncCollection`, so omitted lines are removed and existing lines are re-copied. That is the wrong API for “add one line.”

Also [`BillLineItemResource.setItemPrice`](../omod/src/main/java/org/openmrs/module/billing/web/rest/resource/BillLineItemResource.java) previously was a stub.

## Design rule: snapshot, do not live-link

Bill line items stay **immutable** after creation. `priceUuid` is only a **create-time lookup** into `CashierItemPrice`:

- Resolve the price row once
- Copy `price` amount, `priceName`, and catalog owner (`billableService` / `billableDrug` / `item`) onto the `BillLineItem`
- **Do not set / do not rely on** `BillLineItem.itemPrice` for REST creates — later catalog price edits must not change billed lines
- After create, the denormalized `BillLineItem.price` / `priceName` are the source of truth (enforced by existing immutability interceptor)

`priceUuid` is **write-only** on create. Responses expose the snapped `price`, `priceName`, and catalog refs — not a live price link.

### CashierItemPrice covers services and drugs

One append/create path for all catalog types. `CashierItemPrice` already has exactly one of:

- `billableService` — service prices (`servicePrices`)
- `billableDrug` — drug prices (`drugPrices`)
- `item` — legacy StockItem

The snapshot helper copies whichever owner is non-null onto the line item. No separate service vs drug line-item endpoints.

## API contract

**Append (new):**

```http
POST /ws/rest/v1/billing/bill/{billUuid}/lineItem
```

```json
{ "quantity": 1, "priceUuid": "<cashier-item-price-uuid>" }
```

Optional: `status`, `lineItemOrder`, `batchNumber`.

**Bill create / nested lineItems:**

```json
{
  "patient": "...",
  "lineItems": [
    { "quantity": 1, "priceUuid": "<cashier-item-price-uuid>" }
  ]
}
```

Numeric `price` is **not** creatable via REST. Clients send `priceUuid`; the server snapshots amount and name at create time.

## Implementation notes

- Shared helper: [`BillLineItemPriceSnapshot`](../api/src/main/java/org/openmrs/module/billing/api/util/BillLineItemPriceSnapshot.java)
- Append controller: [`BillLineItemAppendController`](../omod/src/main/java/org/openmrs/module/billing/web/rest/controller/BillLineItemAppendController.java) (custom controller to avoid SubResource converter clash)
- Nested create: [`BillLineItemResource`](../omod/src/main/java/org/openmrs/module/billing/web/rest/resource/BillLineItemResource.java) `priceUuid` setter
- Missing `lineItemOrder` on nested create: [`BillResource.setBillLineItems`](../omod/src/main/java/org/openmrs/module/billing/web/rest/resource/BillResource.java) calls `recalculateLineItemOrder()`

## Out of scope

- Frontend `updateBill` call site changes (consumer app)
- Migrating order-billing strategies off `setItemPrice`
- Dropping the `price_id` column / `itemPrice` mapping from the schema
