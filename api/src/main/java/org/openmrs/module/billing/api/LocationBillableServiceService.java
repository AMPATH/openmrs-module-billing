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

import java.math.BigDecimal;
import java.util.List;

import org.openmrs.annotation.Authorized;
import org.openmrs.Location;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.api.model.LocationBillableService;
import org.openmrs.module.billing.api.util.PrivilegeConstants;

/**
 * Service for per-location billable service assignments, price overrides and effective price
 * resolution.
 *
 * @see LocationBillableService
 * @see BillableServiceGroup
 */
public interface LocationBillableServiceService extends OpenmrsService {
	
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	LocationBillableService getLocationBillableService(Integer id);
	
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	LocationBillableService getLocationBillableServiceByUuid(String uuid);
	
	/**
	 * Lists location-scoped billable service mappings.
	 *
	 * @param location optional location filter (null = all locations)
	 * @param serviceGroup optional group filter (null = all groups)
	 * @param billableService optional single-service filter (null = all services)
	 * @param includeVoided whether voided mappings should be included
	 * @param pagingInfo optional paging information (can be null)
	 */
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	List<LocationBillableService> getBillableServicesByLocation(Location location, BillableServiceGroup serviceGroup,
	        BillableService billableService, boolean includeVoided, PagingInfo pagingInfo);
	
	/**
	 * Convenience overload: enabled (non-voided) services at the given location.
	 */
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	List<LocationBillableService> getBillableServicesByLocation(Location location);
	
	/**
	 * Convenience overload: enabled services at the given location filtered by group.
	 */
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	List<LocationBillableService> getBillableServicesByLocationAndGroup(Location location,
	        BillableServiceGroup serviceGroup);
	
	/**
	 * Saves a mapping after duplicate validation. A (location, billableService) pair may only have one
	 * non-voided mapping.
	 *
	 * @throws IllegalArgumentException if the mapping is invalid or duplicates an existing one
	 * @throws org.openmrs.api.APIAuthenticationException if the user lacks MANAGE_METADATA privilege
	 */
	@Authorized(PrivilegeConstants.MANAGE_METADATA)
	LocationBillableService saveLocationBillableService(LocationBillableService locationBillableService);
	
	/**
	 * Voids (soft-deletes) a mapping with a reason. Named {@code void*} so OpenMRS's standard
	 * RequiredDataAdvice populates the voided-by/date-voided audit fields automatically.
	 */
	@Authorized(PrivilegeConstants.PURGE_METADATA)
	LocationBillableService voidLocationBillableService(LocationBillableService locationBillableService, String reason);
	
	@Authorized(PrivilegeConstants.PURGE_METADATA)
	void purgeLocationBillableService(LocationBillableService locationBillableService);
	
	// ------------------------------------------------------------------
	// Billable service groups
	// ------------------------------------------------------------------
	
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	BillableServiceGroup getServiceGroup(Integer id);
	
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	BillableServiceGroup getServiceGroupByUuid(String uuid);
	
	@Authorized(PrivilegeConstants.VIEW_METADATA)
	List<BillableServiceGroup> getServiceGroups(Location location, boolean includeRetired);
	
	@Authorized(PrivilegeConstants.MANAGE_METADATA)
	BillableServiceGroup saveServiceGroup(BillableServiceGroup serviceGroup);
	
	@Authorized(PrivilegeConstants.PURGE_METADATA)
	void purgeServiceGroup(BillableServiceGroup serviceGroup);
	
	// ------------------------------------------------------------------
	// Price resolution
	// ------------------------------------------------------------------
	
	/**
	 * Central price resolution: returns the location-specific override when a non-voided, enabled
	 * mapping with a price exists for the given location; otherwise falls back to the service's default
	 * (first non-retired) {@link CashierItemPrice}.
	 *
	 * @param billableService the service to price (must not be null)
	 * @param location the location context (may be null — behaves like today's default pricing)
	 * @return the effective unit price, or {@code BigDecimal.ZERO} when no price is defined anywhere
	 */
	BigDecimal getEffectivePrice(BillableService billableService, Location location);
	
	/**
	 * Effective-service lookup used by pickers and order billing: if an enabled, non-voided mapping
	 * exists for (service, location) it is returned; otherwise {@code null} so callers can fall back to
	 * default behaviour.
	 */
	LocationBillableService getEffectiveService(BillableService billableService, Location location);
}
