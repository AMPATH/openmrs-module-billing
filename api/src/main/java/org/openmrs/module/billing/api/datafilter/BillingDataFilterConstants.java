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

/**
 * Names and queries used by {@link BillingLocationFilterListener} and by the hibernate filter it
 * sets the parameter for.
 */
public final class BillingDataFilterConstants {
	
	/**
	 * Name of the hibernate filter that scopes billing data to a set of locations. It is registered
	 * with the datafilter module in api/src/main/resources/filters/hibernate/billing_location.json and
	 * declared, with a condition per mapped class, in Bill.hbm.xml, Cashier.hbm.xml and on
	 * {@link org.openmrs.module.billing.api.model.BillDiscount} and
	 * {@link org.openmrs.module.billing.api.model.BillRefund}.
	 */
	public static final String LOCATION_FILTER_NAME = "billing_locationBasedBillingFilter";
	
	public static final String PARAM_ALLOWED_LOCATION_IDS = "allowedLocationIds";
	
	public static final String GP_ENABLED = "billing.dataFilter.enabled";
	
	/**
	 * Read with raw sql rather than through the AdministrationService because this runs while
	 * datafilter is handing out the current session, and a service call at that point re-enters it.
	 * Note that AdministrationDAO.executeSQL runs on the session's jdbc connection rather than through
	 * hibernate, so it neither triggers a flush nor gets filtered. datafilter reads its own
	 * configuration the same way, see org.openmrs.module.datafilter.Util#isFilterDisabled(String). The
	 * property name is a constant, never input.
	 */
	public static final String ENABLED_QUERY = "SELECT property_value FROM global_property WHERE property = '" + GP_ENABLED
	        + "'";
	
	private BillingDataFilterConstants() {
	}
	
}
