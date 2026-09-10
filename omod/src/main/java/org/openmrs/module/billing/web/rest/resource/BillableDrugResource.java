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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.openmrs.Drug;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillableDrugService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.model.BillableDrug;
import org.openmrs.module.billing.api.model.BillableDrugStatus;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.api.search.BillableDrugSearch;
import org.openmrs.module.billing.web.base.resource.BaseRestDataResource;
import org.openmrs.module.billing.web.base.resource.PagingUtil;
import org.openmrs.module.billing.web.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.MetadataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(name = RestConstants.VERSION_1 + CashierResourceController.BILLING_NAMESPACE
        + "/billableDrug", supportedClass = BillableDrug.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillableDrugResource extends MetadataDelegatingCrudResource<BillableDrug> {
	
	@Override
	public BillableDrug newDelegate() {
		return new BillableDrug();
	}
	
	@Override
	public BillableDrug getByUniqueId(String uuid) {
		return Context.getService(BillableDrugService.class).getBillableDrugByUuid(uuid);
	}
	
	@Override
	public void purge(BillableDrug billableDrug, RequestContext requestContext) throws ResponseException {
		Context.getService(BillableDrugService.class).purgeBillableDrug(billableDrug);
	}
	
	@Override
	public BillableDrug save(BillableDrug delegate) {
		return Context.getService(BillableDrugService.class).saveBillableDrug(delegate);
	}
	
	@Override
	protected AlreadyPaged<BillableDrug> doGetAll(RequestContext context) {
		BillableDrugSearch searchTemplate = new BillableDrugSearch();
		BillableDrugService service = Context.getService(BillableDrugService.class);
		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
		List<BillableDrug> results = service.getBillableDrugs(searchTemplate, pagingInfo);
		return new AlreadyPaged<>(context, results, pagingInfo.hasMoreResults());
	}
	
	@Override
	protected AlreadyPaged<BillableDrug> doSearch(RequestContext context) {
		BillableDrugSearch searchTemplate = new BillableDrugSearch();
		String drugUuid = context.getParameter("drug");
		if (Strings.isEmpty(drugUuid)) {
			drugUuid = context.getParameter("drugUuid");
		}
		if (Strings.isNotEmpty(drugUuid)) {
			searchTemplate.setDrugUuid(drugUuid);
		}
		String locationUuid = context.getParameter("location");
		if (Strings.isEmpty(locationUuid)) {
			locationUuid = context.getParameter("locationUuid");
		}
		if (Strings.isNotEmpty(locationUuid)) {
			searchTemplate.setLocationUuid(locationUuid);
		}
		String name = context.getParameter("name");
		if (Strings.isNotEmpty(name)) {
			searchTemplate.setName(name);
		}
		String status = context.getParameter("status");
		if (Strings.isNotEmpty(status)) {
			searchTemplate.setStatus(BillableDrugStatus.valueOf(status.toUpperCase()));
		} else {
			searchTemplate.setStatus(BillableDrugStatus.ENABLED);
		}
		
		BillableDrugService service = Context.getService(BillableDrugService.class);
		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
		List<BillableDrug> results = service.getBillableDrugs(searchTemplate, pagingInfo);
		return new AlreadyPaged<>(context, results, false);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("name");
			description.addProperty("shortName");
			description.addProperty("drug", Representation.REF);
			description.addProperty("location", Representation.REF);
			description.addProperty("drugPrices");
			description.addProperty("status");
		} else if (rep instanceof CustomRepresentation) {
			description = null;
		}
		return description;
	}
	
	@PropertyGetter("drugPrices")
	public List<CashierItemPrice> getDrugPrices(BillableDrug instance) {
		if (instance.getDrugPrices() == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(instance.getDrugPrices());
	}
	
	@PropertySetter("drugPrices")
	public void setDrugPrices(BillableDrug instance, List<CashierItemPrice> itemPrices) {
		if (instance.getDrugPrices() == null) {
			instance.setDrugPrices(new ArrayList<>(itemPrices.size()));
		}
		BaseRestDataResource.syncCollection(instance.getDrugPrices(), itemPrices);
		for (CashierItemPrice itemPrice : instance.getDrugPrices()) {
			itemPrice.setBillableDrug(instance);
		}
	}
	
	@PropertySetter("drug")
	public void setDrug(BillableDrug instance, String uuid) {
		Drug drug = Context.getConceptService().getDrugByUuid(uuid);
		instance.setDrug(drug);
	}
	
	@PropertySetter("location")
	public void setLocation(BillableDrug instance, String uuid) {
		Location location = Context.getLocationService().getLocationByUuid(uuid);
		instance.setLocation(location);
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("name");
		description.addProperty("shortName");
		description.addProperty("drug");
		description.addProperty("location");
		description.addProperty("drugPrices");
		description.addProperty("status");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		return getCreatableProperties();
	}
}
