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

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.allowedlocation.AllowedLocationAccess;
import org.openmrs.module.allowedlocation.AllowedLocationAccessUtil;
import org.openmrs.module.datafilter.DataFilterContext;
import org.openmrs.module.datafilter.DataFilterListener;
import org.springframework.stereotype.Component;

/**
 * Sets the parameter for {@link BillingDataFilterConstants#LOCATION_FILTER_NAME} from the locations
 * the authenticated user is allowed. <pre>
 * Which locations those are is not decided here: the allowedlocation module resolves them from the
 * user property it also scopes the login location picker with, so that one user property and one set
 * of global properties drive both, see AllowedLocationAccessUtil. All this listener adds is the
 * {@value BillingDataFilterConstants#GP_ENABLED} switch, which keeps billing data unscoped until an
 * administrator turns it on.
 *
 * This is a datafilter {@link DataFilterListener}, discovered by datafilter across every module's
 * classpath via Context.getRegisteredComponents(DataFilterListener.class), the same way its own
 * built-in listeners are. The bean is gated on datafilter alone, and on nothing else, because the
 * two have to arrive together: datafilter enables every filter registered in
 * filters/hibernate/*.json on each session and expects a listener to set the parameters, so a
 * registration whose listener is missing leaves an enabled filter with an unset parameter, which
 * hibernate rejects on the next query. It cannot simply always be registered either, since the class
 * does not load without datafilter on the classpath. The allowedlocation module is a softer
 * dependency, checked at runtime below, and none of this has any effect while
 * {@value BillingDataFilterConstants#GP_ENABLED} is false.
 * </pre>
 *
 * @see BillingDataFilterConstants#LOCATION_FILTER_NAME
 */
@Slf4j
@Component("billingLocationFilterListener")
//Deliberately open ended: a datafilter version this profile did not match would still read the
//filter registration while leaving this listener unregistered, and an enabled filter whose parameter
//nothing sets fails every query, see the class comment above.
@OpenmrsProfile(modules = { "datafilter:2.2.0+" })
public class BillingLocationFilterListener implements DataFilterListener {
	
	private static final String ALLOWED_LOCATION_MODULE_ID = "allowedlocation";
	
	/**
	 * @see DataFilterListener#supports(String)
	 */
	@Override
	public boolean supports(String filterName) {
		return BillingDataFilterConstants.LOCATION_FILTER_NAME.equals(filterName);
	}
	
	/**
	 * @see DataFilterListener#onEnableFilter(DataFilterContext)
	 */
	@Override
	public boolean onEnableFilter(DataFilterContext filterContext) {
		if (!BillingDataFilterSettings.isScopingEnabled()) {
			log.trace("Billing location scoping is turned off, leaving the billing location filter disabled");
			
			return false;
		}
		
		if (!ModuleFactory.isModuleStarted(ALLOWED_LOCATION_MODULE_ID)) {
			log.warn(
			    "{} is set to true but the {} module, which resolves the locations a user is allowed, is not "
			            + "running, leaving billing data unscoped",
			    BillingDataFilterConstants.GP_ENABLED, ALLOWED_LOCATION_MODULE_ID);
			
			return false;
		}
		
		Set<Integer> allowedLocationIds = AllowedLocations.forAuthenticatedUser();
		if (allowedLocationIds == null) {
			log.trace("Leaving the billing location filter disabled, the user is not restricted to any location");
			
			return false;
		}
		
		log.debug("Filtering billing data on location id(s): {}", allowedLocationIds);
		
		filterContext.setParameter(BillingDataFilterConstants.PARAM_ALLOWED_LOCATION_IDS, allowedLocationIds);
		
		return true;
	}
	
	/**
	 * Keeps every reference to the allowedlocation module's api inside a class of its own, which the
	 * jvm only loads when one of its methods is first called. That way the listener itself still loads,
	 * and reports the module as missing, on a server where allowedlocation is not installed at all.
	 */
	private static final class AllowedLocations {
		
		/**
		 * @return the location ids to scope billing data to, or null if the authenticated user is not
		 *         restricted to any location
		 */
		private static Set<Integer> forAuthenticatedUser() {
			AllowedLocationAccess access = AllowedLocationAccessUtil.getAccessibleLocations();
			if (!access.isRestricted()) {
				return null;
			}
			
			return AllowedLocationAccessUtil.asFilterParameter(access);
		}
		
	}
	
}
