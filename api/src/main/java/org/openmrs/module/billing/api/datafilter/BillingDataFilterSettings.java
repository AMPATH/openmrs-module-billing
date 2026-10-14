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

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.api.db.AdministrationDAO;

/**
 * The billing side switch for location scoping of billing data. Which locations a user is allowed
 * is not configured here, the allowedlocation module resolves that, see
 * {@link BillingLocationFilterListener}.
 */
public final class BillingDataFilterSettings {
	
	private BillingDataFilterSettings() {
	}
	
	/**
	 * @return true if {@value BillingDataFilterConstants#GP_ENABLED} is set to true, false when it is
	 *         set to anything else and when it has never been set, so that an installation that has not
	 *         asked for scoping keeps behaving exactly as it did before
	 * @see BillingDataFilterConstants#ENABLED_QUERY for why this is read with raw sql
	 */
	public static boolean isScopingEnabled() {
		AdministrationDAO adminDAO = Context.getRegisteredComponent("adminDAO", AdministrationDAO.class);
		List<List<Object>> rows = adminDAO.executeSQL(BillingDataFilterConstants.ENABLED_QUERY, true);
		if (rows.isEmpty() || rows.get(0).isEmpty() || rows.get(0).get(0) == null) {
			return false;
		}
		
		return Boolean.parseBoolean(rows.get(0).get(0).toString().trim());
	}
	
}
