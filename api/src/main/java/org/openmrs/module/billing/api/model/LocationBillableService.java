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

import java.math.BigDecimal;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Location;

/**
 * Maps a {@link BillableService} to a {@link Location}, optionally overriding its default price,
 * enabling/disabling it at that location, assigning it to a {@link BillableServiceGroup} and
 * controlling its display order.
 * <p>
 * This is the per-location partitioning table for billable services: one row per (location,
 * billable service) pair, enforced by a unique constraint.
 */
public class LocationBillableService extends BaseOpenmrsData {
	
	private static final long serialVersionUID = 0L;
	
	private Integer locationBillableServiceId;
	
	private Location location;
	
	private BillableService billableService;
	
	private BillableServiceGroup serviceGroup;
	
	/**
	 * Optional location-specific price override; when {@code null} the service's default price applies.
	 */
	private BigDecimal priceOverride;
	
	/**
	 * Whether this service is enabled at the mapped location. Defaults to true so legacy rows behave
	 * like today's behaviour.
	 */
	private Boolean enabled = Boolean.TRUE;
	
	private Integer displayOrder = 0;
	
	public Integer getLocationBillableServiceId() {
		return locationBillableServiceId;
	}
	
	public void setLocationBillableServiceId(Integer locationBillableServiceId) {
		this.locationBillableServiceId = locationBillableServiceId;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public BillableService getBillableService() {
		return billableService;
	}
	
	public void setBillableService(BillableService billableService) {
		this.billableService = billableService;
	}
	
	public BillableServiceGroup getServiceGroup() {
		return serviceGroup;
	}
	
	public void setServiceGroup(BillableServiceGroup serviceGroup) {
		this.serviceGroup = serviceGroup;
	}
	
	public BigDecimal getPriceOverride() {
		return priceOverride;
	}
	
	public void setPriceOverride(BigDecimal priceOverride) {
		this.priceOverride = priceOverride;
	}
	
	public Boolean getEnabled() {
		return enabled == null ? Boolean.TRUE : enabled;
	}
	
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
	public Integer getDisplayOrder() {
		return displayOrder == null ? 0 : displayOrder;
	}
	
	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
	
	@Override
	public Integer getId() {
		return getLocationBillableServiceId();
	}
	
	@Override
	public void setId(Integer id) {
		setLocationBillableServiceId(id);
	}
}
