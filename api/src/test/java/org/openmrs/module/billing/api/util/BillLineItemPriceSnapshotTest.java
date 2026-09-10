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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.CashierItemPriceService;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.model.BillableDrug;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.CashierItemPrice;

/**
 * Unit tests for {@link BillLineItemPriceSnapshot}.
 */
public class BillLineItemPriceSnapshotTest {
	
	private CashierItemPriceService priceService;
	
	private MockedStatic<Context> contextMock;
	
	@BeforeEach
	public void setUp() {
		priceService = mock(CashierItemPriceService.class);
		contextMock = mockStatic(Context.class);
		contextMock.when(() -> Context.getService(CashierItemPriceService.class)).thenReturn(priceService);
	}
	
	@AfterEach
	public void tearDown() {
		if (contextMock != null) {
			contextMock.close();
		}
	}
	
	@Test
	public void apply_shouldSnapshotServicePriceWithoutItemPriceLink() {
		BillableService service = new BillableService();
		service.setName("Consultation");
		CashierItemPrice price = new CashierItemPrice();
		price.setName("Cash");
		price.setPrice(BigDecimal.valueOf(75));
		price.setBillableService(service);
		
		BillLineItem lineItem = new BillLineItem();
		BillLineItemPriceSnapshot.apply(lineItem, price);
		
		assertEquals(BigDecimal.valueOf(75), lineItem.getPrice());
		assertEquals("Cash", lineItem.getPriceName());
		assertSame(service, lineItem.getBillableService());
		assertNull(lineItem.getBillableDrug());
		assertNull(lineItem.getItem());
		assertNull(lineItem.getItemPrice());
	}
	
	@Test
	public void apply_shouldSnapshotDrugPrice() {
		BillableDrug drug = new BillableDrug();
		drug.setName("Triomune-30");
		CashierItemPrice price = new CashierItemPrice();
		price.setName("MPESA");
		price.setPrice(BigDecimal.valueOf(150));
		price.setBillableDrug(drug);
		
		BillLineItem lineItem = new BillLineItem();
		BillLineItemPriceSnapshot.apply(lineItem, price);
		
		assertEquals(BigDecimal.valueOf(150), lineItem.getPrice());
		assertEquals("MPESA", lineItem.getPriceName());
		assertSame(drug, lineItem.getBillableDrug());
		assertNull(lineItem.getBillableService());
		assertNull(lineItem.getItemPrice());
	}
	
	@Test
	public void apply_shouldKeepSnapshottedAmountWhenCatalogPriceChanges() {
		BillableService service = new BillableService();
		CashierItemPrice price = new CashierItemPrice();
		price.setName("Cash");
		price.setPrice(BigDecimal.valueOf(75));
		price.setBillableService(service);
		
		BillLineItem lineItem = new BillLineItem();
		BillLineItemPriceSnapshot.apply(lineItem, price);
		
		price.setPrice(BigDecimal.valueOf(999));
		assertEquals(BigDecimal.valueOf(75), lineItem.getPrice());
	}
	
	@Test
	public void apply_shouldRejectRetiredPrice() {
		CashierItemPrice price = new CashierItemPrice();
		price.setRetired(true);
		price.setPrice(BigDecimal.TEN);
		price.setBillableService(new BillableService());
		
		assertThrows(APIException.class, () -> BillLineItemPriceSnapshot.apply(new BillLineItem(), price));
	}
	
	@Test
	public void apply_shouldRejectPriceWithoutCatalogOwner() {
		CashierItemPrice price = new CashierItemPrice();
		price.setPrice(BigDecimal.TEN);
		price.setName("Orphan");
		
		assertThrows(APIException.class, () -> BillLineItemPriceSnapshot.apply(new BillLineItem(), price));
	}
	
	@Test
	public void create_shouldBuildPendingLineFromPriceUuid() {
		BillableService service = new BillableService();
		CashierItemPrice price = new CashierItemPrice();
		price.setUuid("price-uuid");
		price.setName("Cash");
		price.setPrice(BigDecimal.valueOf(50));
		price.setBillableService(service);
		when(priceService.getCashierItemPriceByUuid("price-uuid")).thenReturn(price);
		
		BillLineItem lineItem = BillLineItemPriceSnapshot.create("price-uuid", 2);
		
		assertEquals(2, lineItem.getQuantity());
		assertEquals(BillLineItemStatus.PENDING, lineItem.getStatus());
		assertEquals(BigDecimal.valueOf(50), lineItem.getPrice());
		assertSame(service, lineItem.getBillableService());
		assertNull(lineItem.getItemPrice());
	}
	
	@Test
	public void create_shouldRejectNonPositiveQuantity() {
		assertThrows(APIException.class, () -> BillLineItemPriceSnapshot.create("price-uuid", 0));
	}
	
	@Test
	public void applyFromPriceUuid_shouldRejectMissingPrice() {
		when(priceService.getCashierItemPriceByUuid("missing")).thenReturn(null);
		assertThrows(APIException.class, () -> BillLineItemPriceSnapshot.applyFromPriceUuid(new BillLineItem(), "missing"));
	}
}
