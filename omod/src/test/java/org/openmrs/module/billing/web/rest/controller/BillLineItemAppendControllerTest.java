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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.CashierItemPriceService;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillStatus;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link BillLineItemAppendController}.
 */
public class BillLineItemAppendControllerTest {
	
	private BillLineItemAppendController controller;
	
	private BillService billService;
	
	private CashierItemPriceService priceService;
	
	private MockedStatic<Context> contextMock;
	
	private MockedStatic<ConversionUtil> conversionMock;
	
	@BeforeEach
	public void setUp() {
		controller = new BillLineItemAppendController();
		billService = mock(BillService.class);
		priceService = mock(CashierItemPriceService.class);
		
		contextMock = mockStatic(Context.class);
		contextMock.when(() -> Context.getService(BillService.class)).thenReturn(billService);
		contextMock.when(() -> Context.getService(CashierItemPriceService.class)).thenReturn(priceService);
		
		conversionMock = mockStatic(ConversionUtil.class);
		conversionMock.when(() -> ConversionUtil.convertToRepresentation(any(), eq(Representation.DEFAULT)))
		        .thenReturn(new HashMap<String, Object>());
	}
	
	@AfterEach
	public void tearDown() {
		if (conversionMock != null) {
			conversionMock.close();
		}
		if (contextMock != null) {
			contextMock.close();
		}
	}
	
	@Test
	public void addLineItem_shouldAppendWithoutReplacingExistingLines() throws Exception {
		Bill bill = new Bill();
		bill.setUuid("bill-uuid");
		bill.setId(1);
		bill.setStatus(BillStatus.PENDING);
		BillLineItem existing = new BillLineItem();
		existing.setId(10);
		existing.setUuid("existing-line");
		existing.setLineItemOrder(0);
		bill.addLineItem(existing);
		
		BillableService service = new BillableService();
		CashierItemPrice price = new CashierItemPrice();
		price.setUuid("price-uuid");
		price.setName("Cash");
		price.setPrice(BigDecimal.valueOf(75));
		price.setBillableService(service);
		
		when(billService.getBillByUuid("bill-uuid")).thenReturn(bill);
		when(priceService.getCashierItemPriceByUuid("price-uuid")).thenReturn(price);
		when(billService.saveBill(bill)).thenReturn(bill);
		
		Map<String, Object> body = new HashMap<>();
		body.put("quantity", 1);
		body.put("priceUuid", "price-uuid");
		
		ResponseEntity<Object> response = controller.addLineItem("bill-uuid", body);
		
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(2, bill.getLineItems().size());
		assertSame(existing, bill.getLineItems().get(0));
		BillLineItem added = bill.getLineItems().get(1);
		assertEquals(BigDecimal.valueOf(75), added.getPrice());
		assertEquals("Cash", added.getPriceName());
		assertSame(service, added.getBillableService());
		assertNull(added.getItemPrice());
		assertEquals(1, added.getLineItemOrder());
		verify(billService).saveBill(bill);
	}
	
	@Test
	public void addLineItem_shouldRejectWhenBillDoesNotAcceptNewLines() throws Exception {
		Bill bill = new Bill();
		bill.setUuid("bill-uuid");
		bill.setId(1);
		bill.setStatus(BillStatus.PAID);
		when(billService.getBillByUuid("bill-uuid")).thenReturn(bill);
		
		Map<String, Object> body = new HashMap<>();
		body.put("quantity", 1);
		body.put("priceUuid", "price-uuid");
		
		ResponseEntity<Object> response = controller.addLineItem("bill-uuid", body);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> wrapper = (Map<String, Object>) response.getBody();
		@SuppressWarnings("unchecked")
		Map<String, Object> error = (Map<String, Object>) wrapper.get("error");
		assertTrue(error.get("message").toString().contains("does not accept new line items"));
	}
	
	@Test
	public void addLineItem_shouldReturnNotFoundWhenBillMissing() throws Exception {
		when(billService.getBillByUuid("missing")).thenReturn(null);
		Map<String, Object> body = new HashMap<>();
		body.put("quantity", 1);
		body.put("priceUuid", "price-uuid");
		ResponseEntity<Object> response = controller.addLineItem("missing", body);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}
}
