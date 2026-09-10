/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.util;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.CashierItemPriceService;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.model.CashierItemPrice;

/**
 * Create-time snapshot of a {@link CashierItemPrice} onto a {@link BillLineItem}.
 * <p>
 * Copies amount, name, and catalog owner onto the line item. Does <strong>not</strong> set
 * {@code itemPrice}, so later catalog price changes cannot alter billed lines.
 */
public final class BillLineItemPriceSnapshot {
	
	private BillLineItemPriceSnapshot() {
	}
	
	/**
	 * Loads the price by UUID and snapshots it onto {@code lineItem}.
	 *
	 * @param lineItem the line item to populate
	 * @param priceUuid CashierItemPrice UUID
	 * @throws APIException if uuid blank, price missing/retired, or price has no catalog owner
	 */
	public static void applyFromPriceUuid(BillLineItem lineItem, String priceUuid) {
		if (StringUtils.isBlank(priceUuid)) {
			throw new APIException("priceUuid is required");
		}
		CashierItemPrice price = Context.getService(CashierItemPriceService.class).getCashierItemPriceByUuid(priceUuid);
		apply(lineItem, price);
	}
	
	/**
	 * Snapshots {@code price} onto {@code lineItem} without linking {@code itemPrice}.
	 *
	 * @param lineItem the line item to populate
	 * @param price the catalog price row to snapshot
	 * @throws APIException if price is null, retired, has no amount, or has no catalog owner
	 */
	public static void apply(BillLineItem lineItem, CashierItemPrice price) {
		if (lineItem == null) {
			throw new APIException("lineItem must not be null");
		}
		if (price == null) {
			throw new APIException("CashierItemPrice not found");
		}
		if (Boolean.TRUE.equals(price.getRetired())) {
			throw new APIException("Cannot create a line item from a retired price");
		}
		if (price.getPrice() == null) {
			throw new APIException("CashierItemPrice has no amount");
		}
		
		lineItem.setPrice(price.getPrice());
		lineItem.setPriceName(price.getName());
		
		// Clear any prior catalog refs, then set exactly one from the price row.
		lineItem.setBillableService(null);
		lineItem.setBillableDrug(null);
		lineItem.setItem(null);
		
		if (price.getBillableService() != null) {
			lineItem.setBillableService(price.getBillableService());
		} else if (price.getBillableDrug() != null) {
			lineItem.setBillableDrug(price.getBillableDrug());
		} else if (price.getItem() != null) {
			lineItem.setItem(price.getItem());
		} else {
			throw new APIException("CashierItemPrice has no billableService, billableDrug, or item");
		}
		
		// Intentionally do not set itemPrice — snapshot only for immutability.
	}
	
	/**
	 * Builds a new line item from a price UUID and quantity.
	 *
	 * @param priceUuid CashierItemPrice UUID
	 * @param quantity quantity (must be positive)
	 * @return a populated line item (not yet attached to a bill)
	 */
	public static BillLineItem create(String priceUuid, Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new APIException("quantity must be a positive integer");
		}
		BillLineItem lineItem = new BillLineItem();
		applyFromPriceUuid(lineItem, priceUuid);
		lineItem.setQuantity(quantity);
		if (lineItem.getStatus() == null) {
			lineItem.setStatus(BillLineItemStatus.PENDING);
		}
		return lineItem;
	}
}
