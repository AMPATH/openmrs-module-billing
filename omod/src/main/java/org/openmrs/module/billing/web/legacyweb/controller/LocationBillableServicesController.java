/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.web.legacyweb.controller;

import java.util.List;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.ModuleSettings;
import org.openmrs.module.billing.api.LocationBillableServiceService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.util.PrivilegeConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin page controller for managing per-location billable service groups and assignments (price
 * overrides, enabled flags, display order). The view lists groups/assignments for a selected
 * location; edits go through the REST layer so validation stays in one place.
 */
@Controller
@RequestMapping("/module/billing/locationBillableServices")
public class LocationBillableServicesController {
	
	@RequestMapping(method = RequestMethod.GET)
	public void page(ModelMap model, @RequestParam(value = "locationId", required = false) Integer locationId) {
		Context.requirePrivilege(PrivilegeConstants.VIEW_METADATA);
		
		List<Location> locations = Context.getLocationService().getAllLocations();
		model.addAttribute("locations", locations);
		
		// Default selection: requested location > session location > first location
		Location selected = null;
		if (locationId != null) {
			selected = Context.getLocationService().getLocation(locationId);
		}
		if (selected == null && Context.getUserContext() != null) {
			selected = Context.getUserContext().getLocation();
		}
		if (selected == null && !locations.isEmpty()) {
			selected = locations.get(0);
		}
		model.addAttribute("selectedLocation", selected);
		model.addAttribute("defaultLocationUuid",
		    Context.getAdministrationService().getGlobalProperty(ModuleSettings.DEFAULT_LOCATION_UUID));
		
		if (selected != null) {
			LocationBillableServiceService service = Context.getService(LocationBillableServiceService.class);
			List<BillableServiceGroup> groups = service.getServiceGroups(selected, false);
			model.addAttribute("serviceGroups", groups);
			model.addAttribute("assignments", service.getBillableServicesByLocationAndGroup(selected, null));
		}
	}
}
