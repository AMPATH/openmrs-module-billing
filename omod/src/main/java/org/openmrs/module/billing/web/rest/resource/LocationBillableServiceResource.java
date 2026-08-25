/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.web.rest.resource;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.LocationBillableServiceService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.model.LocationBillableService;
import org.openmrs.module.billing.web.base.resource.PagingUtil;
import org.openmrs.module.billing.web.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * REST resource for per-location billable service assignments and price overrides.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.BILLING_NAMESPACE
        + "/locationBillableService", supportedClass = LocationBillableService.class, supportedOpenmrsVersions = {
                "2.0 - 2.*" })
public class LocationBillableServiceResource extends DelegatingCrudResource<LocationBillableService> {
	
	@Override
	public LocationBillableService newDelegate() {
		return new LocationBillableService();
	}
	
	@Override
	public LocationBillableService getByUniqueId(String uuid) {
		return getService().getLocationBillableServiceByUuid(uuid);
	}
	
	@Override
	public LocationBillableService save(LocationBillableService delegate) {
		return getService().saveLocationBillableService(delegate);
	}
	
	@Override
	public void purge(LocationBillableService delegate, RequestContext requestContext) throws ResponseException {
		getService().purgeLocationBillableService(delegate);
	}
	
	@Override
	protected void delete(LocationBillableService delegate, String reason, RequestContext requestContext)
	        throws ResponseException {
		if (delegate.getVoided()) {
			return;
		}
		getService().voidLocationBillableService(delegate, reason);
	}
	
	@Override
	protected AlreadyPaged<LocationBillableService> doGetAll(RequestContext context) {
		return doSearch(context);
	}
	
	/**
	 * Search parameters: location=<uuid> (required for scoped queries), group=<uuid>, service=<uuid>,
	 * includeAll=true to include voided mappings.
	 */
	@Override
	protected AlreadyPaged<LocationBillableService> doSearch(RequestContext context) {
		String locationUuid = context.getParameter("location");
		String groupUuid = context.getParameter("group");
		String serviceUuid = context.getParameter("service");
		boolean includeAll = Boolean.parseBoolean(context.getParameter("includeAll"));
		
		Location location = StringUtils.isNotBlank(locationUuid)
		        ? Context.getLocationService().getLocationByUuid(locationUuid)
		        : null;
		if (StringUtils.isNotBlank(locationUuid) && location == null) {
			throw new IllegalArgumentException("No location found with uuid " + locationUuid);
		}
		
		BillableServiceGroup group = StringUtils.isNotBlank(groupUuid) ? getService().getServiceGroupByUuid(groupUuid)
		        : null;
		BillableService service = StringUtils.isNotBlank(serviceUuid)
		        ? Context.getService(BillableServiceService.class).getBillableServiceByUuid(serviceUuid)
		        : null;
		
		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
		List<LocationBillableService> mappings = getService().getBillableServicesByLocation(location, group, service,
		    includeAll, pagingInfo);
		
		return new AlreadyPaged<>(context, mappings, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		if (rep instanceof RefRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("display", findMethod("getDisplayString"));
			description.addProperty("voided");
			return description;
		} else if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			DelegatingResourceDescription description = new DelegatingResourceDescription();
			description.addProperty("uuid");
			description.addProperty("location", Representation.REF);
			description.addProperty("billableService",
			    rep instanceof FullRepresentation ? Representation.FULL : Representation.REF);
			description.addProperty("serviceGroup", Representation.REF);
			description.addProperty("priceOverride");
			description.addProperty("enabled");
			description.addProperty("displayOrder");
			description.addProperty("auditInfo", rep instanceof FullRepresentation ? Representation.FULL : null);
			description.addProperty("voided");
			return description;
		}
		return null;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("location");
		description.addProperty("billableService");
		description.addProperty("serviceGroup");
		description.addProperty("priceOverride");
		description.addProperty("enabled");
		description.addProperty("displayOrder");
		description.addProperty("uuid");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		return getCreatableProperties();
	}
	
	@PropertyGetter("display")
	public String getDisplayString(LocationBillableService mapping) {
		if (mapping == null || mapping.getBillableService() == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(mapping.getBillableService().getName());
		if (mapping.getPriceOverride() != null) {
			sb.append(": ").append(mapping.getPriceOverride());
		}
		return sb.toString();
	}
	
	private LocationBillableServiceService getService() {
		return Context.getService(LocationBillableServiceService.class);
	}
}
