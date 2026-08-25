/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.db;

import javax.annotation.Nonnull;

import org.openmrs.Location;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.model.LocationBillableService;

import java.util.List;

/**
 * Data Access Object for {@link LocationBillableService} persistence operations.
 */
public interface LocationBillableServiceDAO {
	
	LocationBillableService getLocationBillableService(@Nonnull Integer id);
	
	LocationBillableService getLocationBillableServiceByUuid(@Nonnull String uuid);
	
	/**
	 * @param location the location (may be null for all locations)
	 * @param serviceGroup optional group filter (may be null)
	 * @param billableService optional single-service filter (may be null)
	 * @param includeRetired whether voided mappings should be included
	 * @param pagingInfo optional paging information
	 */
	List<LocationBillableService> getLocationBillableServices(Location location, BillableServiceGroup serviceGroup,
	        BillableService billableService, boolean includeRetired, PagingInfo pagingInfo);
	
	LocationBillableService saveLocationBillableService(@Nonnull LocationBillableService locationBillableService);
	
	void purgeLocationBillableService(@Nonnull LocationBillableService locationBillableService);
}
