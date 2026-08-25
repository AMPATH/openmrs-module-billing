/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.TestConstants;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.model.LocationBillableService;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

/**
 * Integration tests for location-based partitioning: per-location billable service mappings,
 * groups, price precedence and duplicate validation.
 */
public class LocationBillableServiceServiceTest extends BaseModuleContextSensitiveTest {
	
	private static final String LOCATION_A_UUID = "11111111-aaaa-2222-bbbb-333333333301";
	
	private static final String LOCATION_B_UUID = "11111111-aaaa-2222-bbbb-333333333302";
	
	private static final Integer LOCATION_A_ID = 100;
	
	private static final Integer LOCATION_B_ID = 101;
	
	private LocationBillableServiceService service;
	
	private BillableServiceService billableServiceService;
	
	@BeforeEach
	public void setup() throws Exception {
		service = Context.getService(LocationBillableServiceService.class);
		billableServiceService = Context.getService(BillableServiceService.class);
		
		executeDataSet(TestConstants.CORE_DATASET2);
		executeDataSet(TestConstants.BASE_DATASET_DIR + "BillableServiceTest.xml");
		executeDataSet(TestConstants.BASE_DATASET_DIR + "LocationBillableServiceTest.xml");
	}
	
	private Location locationA() {
		return Context.getLocationService().getLocationByUuid(LOCATION_A_UUID);
	}
	
	private Location locationB() {
		return Context.getLocationService().getLocationByUuid(LOCATION_B_UUID);
	}
	
	private BillableService serviceX() {
		return billableServiceService.getBillableService(50);
	}
	
	// ------------------------------------------------------------------
	// Mapping CRUD + validation
	// ------------------------------------------------------------------
	
	@Test
	public void saveLocationBillableService_shouldRejectNullLocation() {
		LocationBillableService mapping = new LocationBillableService();
		mapping.setBillableService(serviceX());
		assertThrows(IllegalArgumentException.class, () -> service.saveLocationBillableService(mapping));
	}
	
	@Test
	public void saveLocationBillableService_shouldRejectDuplicateLocationAndService() {
		LocationBillableService duplicate = new LocationBillableService();
		duplicate.setLocation(locationA());
		duplicate.setBillableService(serviceX());
		duplicate.setPriceOverride(new BigDecimal("1.00"));
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
		    () -> service.saveLocationBillableService(duplicate));
		assertTrue(ex.getMessage().contains("already exists"));
	}
	
	@Test
	public void saveLocationBillableService_shouldAllowSameServiceAtDifferentLocations() {
		// (locationB, serviceX) already exists in the dataset; adding a *different* pair must succeed
		BillableService serviceY = billableServiceService.getBillableService(51);
		LocationBillableService mapping = new LocationBillableService();
		mapping.setLocation(locationB());
		mapping.setBillableService(serviceY);
		mapping = service.saveLocationBillableService(mapping);
		assertNotNull(mapping.getId());
	}
	
	@Test
	public void voidLocationBillableService_shouldRequireReasonAndVoidTheMapping() {
		LocationBillableService original = service.getLocationBillableService(81);
		assertFalse(original.getVoided());
		
		assertThrows(IllegalArgumentException.class, () -> service.voidLocationBillableService(original, null));
		
		LocationBillableService voided = service.voidLocationBillableService(original, "no longer offered");
		assertTrue(voided.getVoided());
		assertEquals("no longer offered", voided.getVoidReason());
	}
	
	@Test
	public void voidThenResave_shouldBeAllowedAfterVoiding() {
		LocationBillableService voided = service.voidLocationBillableService(service.getLocationBillableService(80),
		    "voided for test");
		assertTrue(voided.getVoided());
		
		// The unique constraint only applies to non-voided rows, so re-creating must work
		LocationBillableService fresh = new LocationBillableService();
		fresh.setLocation(locationA());
		fresh.setBillableService(serviceX());
		fresh.setPriceOverride(new BigDecimal("12.00"));
		assertNotNull(service.saveLocationBillableService(fresh).getId());
	}
	
	// ------------------------------------------------------------------
	// Location / group queries
	// ------------------------------------------------------------------
	
	@Test
	public void getBillableServicesByLocation_shouldReturnOnlyMappingsForThatLocation() {
		List<LocationBillableService> atA = service.getBillableServicesByLocation(locationA());
		assertEquals(2, atA.size());
		for (LocationBillableService m : atA) {
			assertEquals(LOCATION_A_ID, m.getLocation().getLocationId());
		}
		
		List<LocationBillableService> atB = service.getBillableServicesByLocation(locationB());
		assertEquals(1, atB.size());
		assertEquals(LOCATION_B_ID, atB.get(0).getLocation().getLocationId());
	}
	
	@Test
	public void getBillableServicesByLocationAndGroup_shouldFilterByGroup() {
		BillableServiceGroup groupA = service.getServiceGroup(70);
		List<LocationBillableService> inGroup = service.getBillableServicesByLocationAndGroup(locationA(), groupA);
		assertEquals(1, inGroup.size());
		assertEquals(Integer.valueOf(50), inGroup.get(0).getBillableService().getBillableServiceId());
	}
	
	@Test
	public void getBillableServicesByLocation_shouldExcludeDisabledMappings() {
		LocationBillableService mapping = service.getLocationBillableService(81);
		mapping.setEnabled(false);
		service.saveLocationBillableService(mapping);
		
		List<LocationBillableService> atA = service.getBillableServicesByLocation(locationA());
		assertEquals(1, atA.size());
	}
	
	// ------------------------------------------------------------------
	// Price precedence
	// ------------------------------------------------------------------
	
	@Test
	public void getEffectivePrice_shouldPreferLocationOverrideOverDefaultPrice() {
		// Service 50 default price is 10.00; location A overrides to 15.50
		assertEquals(new BigDecimal("15.50"), service.getEffectivePrice(serviceX(), locationA()));
	}
	
	@Test
	public void getEffectivePrice_shouldFallBackToDefaultWhenNoOverride() {
		// Service 51 has no mapping at location B: falls back to its default price of 20.00
		BillableService serviceY = billableServiceService.getBillableService(51);
		assertEquals(new BigDecimal("20.00"), service.getEffectivePrice(serviceY, locationB()));
	}
	
	@Test
	public void getEffectivePrice_shouldFallBackToDefaultWhenMappingHasNoOverride() {
		// Mapping 81 exists at A for service 51 but carries no override; default price 20.00 wins
		BillableService serviceY = billableServiceService.getBillableService(51);
		assertEquals(new BigDecimal("20.00"), service.getEffectivePrice(serviceY, locationA()));
	}
	
	@Test
	public void getEffectiveService_shouldReturnNullForUnmappedLocation() {
		Location unmapped = Context.getLocationService().getLocation(999);
		assertNull(service.getEffectiveService(serviceX(), unmapped));
		assertNull(service.getEffectiveService(serviceX(), null));
	}
	
	// ------------------------------------------------------------------
	// Backward compatibility
	// ------------------------------------------------------------------
	
	@Test
	public void getEffectivePrice_withoutAnyMappings_shouldBehaveLikeLegacyPricing() {
		BillableService legacy = billableServiceService.getBillableService(0); // from BillableServiceTest.xml
		assertNotNull(legacy);
		// No LocationBillableService rows reference service 0, so the plain default price path runs
		BigDecimal effective = service.getEffectivePrice(legacy, locationA());
		assertNotNull(effective);
		assertTrue(effective.signum() >= 0);
	}
	
	// ------------------------------------------------------------------
	// Groups
	// ------------------------------------------------------------------
	
	@Test
	public void getServiceGroups_shouldOnlyReturnGroupsAtGivenLocation() {
		List<BillableServiceGroup> atA = service.getServiceGroups(locationA(), false);
		assertEquals(1, atA.size());
		assertEquals("Lab Group A", atA.get(0).getName());
		
		List<BillableServiceGroup> all = service.getServiceGroups(null, false);
		assertEquals(2, all.size());
	}
	
	@Test
	public void saveServiceGroup_shouldRequireName() {
		BillableServiceGroup group = new BillableServiceGroup();
		group.setLocation(locationA());
		assertThrows(IllegalArgumentException.class, () -> service.saveServiceGroup(group));
		
		group.setName("New Group");
		assertNotNull(service.saveServiceGroup(group).getId());
	}
	
	// ------------------------------------------------------------------
	// Bill location partitioning
	// ------------------------------------------------------------------
	
	@Test
	public void getBills_byLocationUuid_shouldReturnOnlyThatLocationsBills() {
		org.openmrs.module.billing.api.search.BillSearch search = new org.openmrs.module.billing.api.search.BillSearch();
		search.setLocationUuid(LOCATION_A_UUID);
		
		List<Bill> billsAtA = Context.getService(BillService.class).getBills(search, new PagingInfo(1, 100));
		assertEquals(1, billsAtA.size());
		assertEquals("loc receipt A", billsAtA.get(0).getReceiptNumber());
		
		search.setLocationUuid(LOCATION_B_UUID);
		List<Bill> billsAtB = Context.getService(BillService.class).getBills(search, new PagingInfo(1, 100));
		assertEquals(1, billsAtB.size());
		assertEquals("loc receipt B", billsAtB.get(0).getReceiptNumber());
	}
	
	@Test
	public void getBills_withoutLocationFilter_shouldIncludeAllBills() {
		org.openmrs.module.billing.api.search.BillSearch search = new org.openmrs.module.billing.api.search.BillSearch();
		List<Bill> bills = Context.getService(BillService.class).getBills(search, new PagingInfo(1, 100));
		// Bills without any location must remain visible when no filter is supplied
		assertTrue(bills.size() >= 3);
	}
}
