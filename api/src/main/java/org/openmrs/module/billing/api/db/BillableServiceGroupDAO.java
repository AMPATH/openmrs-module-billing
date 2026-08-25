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
import org.openmrs.module.billing.api.model.BillableServiceGroup;

import java.util.List;

/**
 * Data Access Object for {@link BillableServiceGroup} persistence operations.
 */
public interface BillableServiceGroupDAO {
	
	BillableServiceGroup getBillableServiceGroup(@Nonnull Integer id);
	
	BillableServiceGroup getBillableServiceGroupByUuid(@Nonnull String uuid);
	
	/**
	 * @param location optional location filter (may be null for all locations)
	 * @param includeRetired whether retired groups should be included
	 */
	List<BillableServiceGroup> getBillableServiceGroups(Location location, boolean includeRetired);
	
	BillableServiceGroup saveBillableServiceGroup(@Nonnull BillableServiceGroup billableServiceGroup);
	
	void purgeBillableServiceGroup(@Nonnull BillableServiceGroup billableServiceGroup);
}
