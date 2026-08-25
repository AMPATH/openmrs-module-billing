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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.ModuleSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Enables the {@code billingLocationFilter} Hibernate filter on the current session so that all
 * queries and lazy collection loads on location-partitioned entities (Bill,
 * LocationBillableService, BillableServiceGroup) are restricted to the authenticated session's
 * location.
 * <p>
 * Scoping is opt-in: it is only active when the global property
 * {@code billing.enforceLocationScope} is {@code true} (default {@code false} keeps behaviour
 * identical to previous releases). When enabled but no session location is set, the filter is left
 * off rather than hiding every row.
 */
@Slf4j
@Component("billing.billingLocationScope")
public class BillingLocationScope {
	
	public static final String FILTER_NAME = "billingLocationFilter";
	
	private static final String PARAMETER_NAME = "locationId";
	
	private final SessionFactory sessionFactory;
	
	@Autowired
	public BillingLocationScope(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @return true when the billing location scope is enforced by configuration.
	 */
	public boolean isEnforced() {
		try {
			String value = Context.getAdministrationService().getGlobalProperty(ModuleSettings.ENFORCE_LOCATION_SCOPE);
			return Boolean.parseBoolean(value);
		}
		catch (Exception e) {
			log.debug("Could not read global property {}; scoping stays off", ModuleSettings.ENFORCE_LOCATION_SCOPE, e);
			return false;
		}
	}
	
	/**
	 * Enables the location filter on the current session when scoping is enforced and a session
	 * location is available. Safe to call repeatedly; also re-applies the filter after
	 * {@code session.disableFilter(...)} calls.
	 *
	 * @return true if the filter is active on the current session after this call
	 */
	public boolean apply() {
		if (!isEnforced()) {
			return false;
		}
		
		Location location = getSessionLocation();
		if (location == null || location.getLocationId() == null) {
			log.debug("billing.enforceLocationScope is on but no session location is set; location filter not applied");
			return false;
		}
		
		Session session = sessionFactory.getCurrentSession();
		Filter filter = session.getEnabledFilter(FILTER_NAME);
		if (filter == null) {
			filter = session.enableFilter(FILTER_NAME);
			filter.setParameter(PARAMETER_NAME, location.getLocationId());
			log.trace("Applied {} with locationId={} on session {}", FILTER_NAME, location.getLocationId(), session);
		}
		return true;
	}
	
	/**
	 * Disables the filter on the current session (used in tests and admin tooling).
	 */
	public void clear() {
		try {
			sessionFactory.getCurrentSession().disableFilter(FILTER_NAME);
		}
		catch (Exception e) {
			log.debug("No session available to clear the location filter", e);
		}
	}
	
	private Location getSessionLocation() {
		if (Context.getUserContext() != null) {
			return Context.getUserContext().getLocation();
		}
		return null;
	}
}
