/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.impl;

import java.math.BigDecimal;
import java.util.List;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.billing.api.ItemPriceService;
import org.openmrs.module.billing.api.LocationBillableServiceService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.db.BillableServiceGroupDAO;
import org.openmrs.module.billing.api.db.LocationBillableServiceDAO;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.api.model.LocationBillableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class LocationBillableServiceServiceImpl extends BaseOpenmrsService implements LocationBillableServiceService {
	
	@Setter(onMethod_ = { @Autowired })
	private LocationBillableServiceDAO locationBillableServiceDAO;
	
	@Setter(onMethod_ = { @Autowired })
	private BillableServiceGroupDAO billableServiceGroupDAO;
	
	@Setter(onMethod_ = { @Autowired })
	private ItemPriceService itemPriceService;
	
	// ------------------------------------------------------------------
	// Mappings
	// ------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public LocationBillableService getLocationBillableService(Integer id) {
		if (id == null) {
			return null;
		}
		return locationBillableServiceDAO.getLocationBillableService(id);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public LocationBillableService getLocationBillableServiceByUuid(String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return null;
		}
		return locationBillableServiceDAO.getLocationBillableServiceByUuid(uuid);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<LocationBillableService> getBillableServicesByLocation(Location location, BillableServiceGroup serviceGroup,
	        BillableService billableService, boolean includeVoided, PagingInfo pagingInfo) {
		return locationBillableServiceDAO.getLocationBillableServices(location, serviceGroup, billableService, includeVoided,
		    pagingInfo);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<LocationBillableService> getBillableServicesByLocation(Location location) {
		return getBillableServicesByLocationAndGroup(location, null);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<LocationBillableService> getBillableServicesByLocationAndGroup(Location location,
	        BillableServiceGroup serviceGroup) {
		List<LocationBillableService> mappings = locationBillableServiceDAO.getLocationBillableServices(location,
		    serviceGroup, null, false, null);
		
		java.util.Iterator<LocationBillableService> it = mappings.iterator();
		while (it.hasNext()) {
			LocationBillableService m = it.next();
			if (!Boolean.TRUE.equals(m.getEnabled())) {
				it.remove();
			}
		}
		return mappings;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public LocationBillableService saveLocationBillableService(LocationBillableService locationBillableService) {
		if (locationBillableService == null) {
			throw new NullPointerException("The locationBillableService must be defined.");
		}
		if (locationBillableService.getLocation() == null) {
			throw new IllegalArgumentException("A location must be defined on the mapping.");
		}
		if (locationBillableService.getBillableService() == null) {
			throw new IllegalArgumentException("A billableService must be defined on the mapping.");
		}
		
		validateNoDuplicateMapping(locationBillableService);
		
		return locationBillableServiceDAO.saveLocationBillableService(locationBillableService);
	}
	
	private void validateNoDuplicateMapping(LocationBillableService candidate) {
		BillableService service = candidate.getBillableService();
		Location location = candidate.getLocation();
		
		List<LocationBillableService> existing = locationBillableServiceDAO.getLocationBillableServices(location, null,
		    service, false, null);
		for (LocationBillableService m : existing) {
			if (candidate.getId() == null || !m.getId().equals(candidate.getId())) {
				throw new IllegalArgumentException(
				        "A mapping for this billable service and location already exists (uuid=" + m.getUuid() + ")");
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public LocationBillableService voidLocationBillableService(LocationBillableService locationBillableService,
	        String reason) {
		if (StringUtils.isEmpty(reason)) {
			throw new IllegalArgumentException("Void reason cannot be empty or null");
		}
		if (locationBillableService == null) {
			throw new NullPointerException("The locationBillableService must be defined.");
		}
		// Set the audit fields here as well as relying on the RequiredDataAdvice "void" pointcut:
		// the advice populates voided-by/date-voided (and may or may not copy the reason depending
		// on platform version), so setting them explicitly keeps behaviour deterministic.
		locationBillableService.setVoided(true);
		if (StringUtils.isEmpty(locationBillableService.getVoidReason())) {
			locationBillableService.setVoidReason(reason);
		}
		return locationBillableServiceDAO.saveLocationBillableService(locationBillableService);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void purgeLocationBillableService(LocationBillableService locationBillableService) {
		if (locationBillableService == null) {
			throw new NullPointerException("The locationBillableService must be defined.");
		}
		locationBillableServiceDAO.purgeLocationBillableService(locationBillableService);
	}
	
	// ------------------------------------------------------------------
	// Groups
	// ------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public BillableServiceGroup getServiceGroup(Integer id) {
		if (id == null) {
			return null;
		}
		return billableServiceGroupDAO.getBillableServiceGroup(id);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public BillableServiceGroup getServiceGroupByUuid(String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return null;
		}
		return billableServiceGroupDAO.getBillableServiceGroupByUuid(uuid);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public List<BillableServiceGroup> getServiceGroups(Location location, boolean includeRetired) {
		return billableServiceGroupDAO.getBillableServiceGroups(location, includeRetired);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public BillableServiceGroup saveServiceGroup(BillableServiceGroup serviceGroup) {
		if (serviceGroup == null) {
			throw new NullPointerException("The serviceGroup must be defined.");
		}
		if (StringUtils.isBlank(serviceGroup.getName())) {
			throw new IllegalArgumentException("A group name must be defined.");
		}
		return billableServiceGroupDAO.saveBillableServiceGroup(serviceGroup);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	public void purgeServiceGroup(BillableServiceGroup serviceGroup) {
		if (serviceGroup == null) {
			throw new NullPointerException("The serviceGroup must be defined.");
		}
		billableServiceGroupDAO.purgeBillableServiceGroup(serviceGroup);
	}
	
	// ------------------------------------------------------------------
	// Price resolution
	// ------------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public BigDecimal getEffectivePrice(BillableService billableService, Location location) {
		if (billableService == null) {
			throw new NullPointerException("The billableService must be defined.");
		}
		
		// 1) Location-specific override
		if (location != null) {
			LocationBillableService mapping = getEffectiveService(billableService, location);
			if (mapping != null && mapping.getPriceOverride() != null) {
				return mapping.getPriceOverride();
			}
		}
		
		// 2) Default price fallback — same resolution as today's billing strategies
		List<CashierItemPrice> itemPrices = itemPriceService.getServicePrice(billableService);
		if (!itemPrices.isEmpty()) {
			return itemPrices.get(0).getPrice();
		}
		return BigDecimal.ZERO;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public LocationBillableService getEffectiveService(BillableService billableService, Location location) {
		if (billableService == null) {
			throw new NullPointerException("The billableService must be defined.");
		}
		if (location == null) {
			return null;
		}
		List<LocationBillableService> mappings = locationBillableServiceDAO.getLocationBillableServices(location, null,
		    billableService, false, null);
		for (LocationBillableService mapping : mappings) {
			if (Boolean.TRUE.equals(mapping.getEnabled())) {
				return mapping;
			}
		}
		return null;
	}
}
