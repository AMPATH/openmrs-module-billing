/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.model;

import org.openmrs.BaseChangeableOpenmrsMetadata;
import org.openmrs.Location;

/**
 * A named group of billable services scoped to a single {@link org.openmrs.Location}.
 * <p>
 * Groups allow billable services to be organised per location (e.g. "Lab - Rural Clinic"). The
 * association between a group and its member services is expressed through
 * {@link LocationBillableService#getServiceGroup()}.
 */
public class BillableServiceGroup extends BaseChangeableOpenmrsMetadata {
	
	private static final long serialVersionUID = 0L;
	
	private Integer billableServiceGroupId;
	
	private String name;
	
	private String description;
	
	private Location location;
	
	public Integer getBillableServiceGroupId() {
		return billableServiceGroupId;
	}
	
	public void setBillableServiceGroupId(Integer billableServiceGroupId) {
		this.billableServiceGroupId = billableServiceGroupId;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	@Override
	public Integer getId() {
		return getBillableServiceGroupId();
	}
	
	@Override
	public void setId(Integer id) {
		setBillableServiceGroupId(id);
	}
}
