/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.datafilter;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.module.billing.TestConstants;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks what {@value BillingDataFilterConstants#LOCATION_FILTER_NAME} returns for a given set of
 * allowed locations, by enabling it on the session the way the datafilter module does at runtime.
 * <pre>
 * Enabling it here rather than going through datafilter keeps these tests about the conditions
 * declared on the billing mappings, which is the part this module owns: the filter is declared for
 * the xml mapped classes in Bill.hbm.xml and Cashier.hbm.xml and with a @Filter annotation on
 * BillDiscount and BillRefund, and all of it has to agree on one parameter name and type. Resolving
 * which locations a user is allowed belongs to the allowedlocation module and is tested there.
 * </pre>
 */
public class BillingLocationFilterTest extends BaseModuleContextSensitiveTest {
	
	/** Cash point 100 is at clinic 101, which is under district 100 */
	private static final Integer SCOPED_CLINIC = 101;
	
	/** Cash point 200 is at hospital 200, cash point 300 is at no location at all */
	private static final Integer OTHER_HOSPITAL = 200;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@BeforeEach
	public void setup() throws Exception {
		executeDataSet(TestConstants.CORE_DATASET2);
		executeDataSet(TestConstants.BASE_DATASET_DIR + "BillingDataFilterTest.xml");
	}
	
	@Test
	public void shouldReturnEveryRowWhileTheFilterIsNotEnabled() {
		assertEquals(asList(100, 101, 200, 201, 300), ids("Bill"));
		assertEquals(asList(100, 200, 300), ids("BillLineItem"));
		assertEquals(asList(100, 200), ids("Payment"));
		assertEquals(asList(100, 200), ids("BillDiscount"));
		assertEquals(asList(100, 200), ids("BillRefund"));
		assertEquals(asList(100, 200, 300), ids("CashPoint"));
		assertEquals(asList(100, 200), ids("Timesheet"));
		assertEquals(asList(100, 200, 300), ids("BillableService"));
		assertEquals(asList(100, 200, 300), ids("BillableDrug"));
		assertEquals(asList(100, 200, 300, 400, 500, 600, 700), ids("CashierItemPrice"));
		assertEquals(asList(100, 200), ids("PaymentLineItemAllocation"));
	}
	
	@Test
	public void shouldOnlyReturnBillsFromTheAllowedLocations() {
		allow(SCOPED_CLINIC);
		
		//Bill 200 and 201 are at the other hospital, bill 300 is at a cash point with no location
		assertEquals(asList(100, 101), ids("Bill"));
	}
	
	@Test
	public void shouldOnlyReturnBillLineItemsOfBillsFromTheAllowedLocations() {
		allow(SCOPED_CLINIC);
		
		assertEquals(Collections.singletonList(100), ids("BillLineItem"));
	}
	
	@Test
	public void shouldOnlyReturnPaymentsOfBillsFromTheAllowedLocations() {
		allow(SCOPED_CLINIC);
		
		assertEquals(Collections.singletonList(100), ids("Payment"));
	}
	
	@Test
	public void shouldOnlyReturnDiscountsOfBillsFromTheAllowedLocations() {
		allow(OTHER_HOSPITAL);
		
		assertEquals(Collections.singletonList(200), ids("BillDiscount"));
	}
	
	@Test
	public void shouldOnlyReturnRefundsOfBillsFromTheAllowedLocations() {
		allow(OTHER_HOSPITAL);
		
		assertEquals(Collections.singletonList(200), ids("BillRefund"));
	}
	
	@Test
	public void shouldOnlyReturnCashPointsAtTheAllowedLocations() {
		allow(SCOPED_CLINIC);
		
		//Cash point 300 has no location, so it is at none of the allowed ones
		assertEquals(Collections.singletonList(100), ids("CashPoint"));
	}
	
	@Test
	public void shouldOnlyReturnTimesheetsOfCashPointsAtTheAllowedLocations() {
		allow(SCOPED_CLINIC);
		
		assertEquals(Collections.singletonList(100), ids("Timesheet"));
	}
	
	@Test
	public void shouldReturnRowsForEveryAllowedLocation() {
		allow(SCOPED_CLINIC, OTHER_HOSPITAL);
		
		assertEquals(asList(100, 101, 200, 201), ids("Bill"));
		assertEquals(asList(100, 200), ids("CashPoint"));
		assertEquals(asList(100, 200, 300), ids("BillableService"));
	}
	
	@Test
	public void shouldReturnNoRowsForAUserThatIsAllowedNothing() {
		//What the listener binds when a user is allowed no location, the filter has to fail closed
		allow(-1);
		
		assertTrue(ids("Bill").isEmpty());
		assertTrue(ids("BillLineItem").isEmpty());
		assertTrue(ids("Payment").isEmpty());
		assertTrue(ids("BillDiscount").isEmpty());
		assertTrue(ids("BillRefund").isEmpty());
		assertTrue(ids("CashPoint").isEmpty());
		assertTrue(ids("Timesheet").isEmpty());
		assertTrue(ids("PaymentLineItemAllocation").isEmpty());
		//The global catalogue entries survive: they belong to no location, so they are not another
		//location's data, and the strategies fall back to them when a location has no entry of its own
		assertEquals(Collections.singletonList(300), ids("BillableService"));
		assertEquals(Collections.singletonList(300), ids("BillableDrug"));
		assertEquals(asList(300, 600, 700), ids("CashierItemPrice"));
	}
	
	@Test
	public void shouldOnlyReturnCatalogueEntriesOfTheAllowedLocationsAndTheGlobalOnes() {
		allow(SCOPED_CLINIC);
		
		//200 belongs to the other hospital, 300 belongs to no location and is available everywhere
		assertEquals(asList(100, 300), ids("BillableService"));
		assertEquals(asList(100, 300), ids("BillableDrug"));
	}
	
	@Test
	public void shouldOnlyReturnThePricesOfTheVisibleCatalogueEntries() {
		allow(SCOPED_CLINIC);
		
		//100 and 400 price the clinic's entries, 300 and 600 the global ones, 700 belongs to no
		//catalogue entry at all; 200 and 500 belong to the other hospital
		assertEquals(asList(100, 300, 400, 600, 700), ids("CashierItemPrice"));
	}
	
	@Test
	public void shouldOnlyReturnPaymentAllocationsOfBillsFromTheAllowedLocations() {
		allow(OTHER_HOSPITAL);
		
		assertEquals(Collections.singletonList(200), ids("PaymentLineItemAllocation"));
	}
	
	@Test
	public void shouldOnlyReturnTheAdjustmentsFromTheAllowedLocations() {
		//Bills 101 and 201 both adjust bill 100, from either side of the location boundary. A class
		//level filter is not applied when hibernate loads a collection, hence the one on the mapping
		//of the collection itself.
		allow(SCOPED_CLINIC);
		
		Bill adjusted = session().createQuery("from Bill where id = 100", Bill.class).uniqueResult();
		Collection<Bill> adjustedBy = adjusted.getAdjustedBy();
		
		assertEquals(1, adjustedBy.size());
		assertEquals(Integer.valueOf(101), adjustedBy.iterator().next().getId());
	}
	
	@Test
	public void shouldNotMakeAQueryThatJoinsTheCashPointAmbiguous() {
		//The conditions leave the filtered table's own column unqualified for hibernate to prefix with
		//the entity alias. This query puts cashier_bill and cashier_cash_point, which both have a
		//cash_point_id column, in one statement, so it fails if that prefixing ever stops happening.
		allow(SCOPED_CLINIC);
		
		List<?> rows = session().createQuery("select b.id from Bill b join b.cashPoint cp where cp.retired = false").list();
		
		assertEquals(2, rows.size());
	}
	
	/**
	 * Enables the filter on the current session with the specified location ids, which is what
	 * datafilter does once per session from {@link BillingLocationFilterListener}.
	 *
	 * @param locationIds the ids to bind to the filter's parameter
	 */
	private void allow(Integer... locationIds) {
		session().enableFilter(BillingDataFilterConstants.LOCATION_FILTER_NAME)
		        .setParameterList(BillingDataFilterConstants.PARAM_ALLOWED_LOCATION_IDS, asList(locationIds));
	}
	
	/**
	 * @param entityName the name of the entity to query
	 * @return the ids of every row of the entity the current session can see, in ascending order
	 */
	private List<Integer> ids(String entityName) {
		return session().createQuery("select e.id from " + entityName + " e order by e.id", Integer.class).list();
	}
	
	private Session session() {
		return sessionFactory.getCurrentSession();
	}
	
}
