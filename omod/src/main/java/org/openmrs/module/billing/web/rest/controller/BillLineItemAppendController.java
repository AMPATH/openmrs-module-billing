/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.web.rest.controller;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.util.BillLineItemPriceSnapshot;
import org.openmrs.module.billing.web.rest.resource.BillLineItemResource;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Appends a single line item to a bill without replacing the existing collection.
 * <p>
 * Implemented as a custom controller (not a SubResource) to avoid a second REST converter for
 * {@link BillLineItem} alongside {@link BillLineItemResource}.
 */
@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/billing/bill/{billUuid}/lineItem")
public class BillLineItemAppendController {
	
	@PostMapping
	public ResponseEntity<Object> addLineItem(@PathVariable String billUuid, @RequestBody Map<String, Object> body)
	        throws ResponseException {
		try {
			if (body == null) {
				throw new APIException("Request body is required");
			}
			
			BillService billService = Context.getService(BillService.class);
			Bill bill = billService.getBillByUuid(billUuid);
			if (bill == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody("Bill not found: " + billUuid));
			}
			if (!bill.acceptsNewLineItems()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				        .body(errorBody("Bill does not accept new line items in status " + bill.getStatus()));
			}
			
			String priceUuid = body.get("priceUuid") != null ? body.get("priceUuid").toString() : null;
			Integer quantity = toInteger(body.get("quantity"));
			
			BillLineItem lineItem = BillLineItemPriceSnapshot.create(priceUuid, quantity);
			
			if (body.get("status") != null) {
				lineItem.setStatus(BillLineItemStatus.valueOf(body.get("status").toString()));
			}
			if (body.get("batchNumber") != null) {
				lineItem.setBatchNumber(body.get("batchNumber").toString());
			}
			if (body.get("lineItemOrder") != null) {
				lineItem.setLineItemOrder(toInteger(body.get("lineItemOrder")));
			} else {
				lineItem.setLineItemOrder(nextLineItemOrder(bill));
			}
			
			bill.addLineItem(lineItem);
			billService.saveBill(bill);
			
			Object representation = ConversionUtil.convertToRepresentation(lineItem, Representation.DEFAULT);
			return ResponseEntity.status(HttpStatus.CREATED).body(representation);
		}
		catch (APIException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
		}
	}
	
	private static Map<String, Object> errorBody(String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("message", message);
		Map<String, Object> wrapper = new HashMap<>();
		wrapper.put("error", error);
		return wrapper;
	}
	
	private static int nextLineItemOrder(Bill bill) {
		int max = -1;
		if (bill.getLineItems() != null) {
			for (BillLineItem existing : bill.getLineItems()) {
				if (existing != null && existing.getLineItemOrder() != null && existing.getLineItemOrder() > max) {
					max = existing.getLineItemOrder();
				}
			}
		}
		return max + 1;
	}
	
	private static Integer toInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return Integer.valueOf(value.toString());
	}
}
