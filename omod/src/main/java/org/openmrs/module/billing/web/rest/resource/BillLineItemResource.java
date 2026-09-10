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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillableDrugService;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.BillLineItemService;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.base.entity.IEntityDataService;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillableDrug;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.util.BillLineItemPriceSnapshot;
import org.openmrs.module.billing.web.base.resource.BaseRestDataResource;
import org.openmrs.module.billing.web.rest.controller.base.CashierResourceController;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;

/**
 * REST resource representing a {@link BillLineItem}.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.BILLING_NAMESPACE
        + "/billLineItem", supportedClass = BillLineItem.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillLineItemResource extends BaseRestDataResource<BillLineItem> {
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("item");
			description.addProperty("billableService", Representation.REF);
			description.addProperty("billableDrug", Representation.REF);
			description.addProperty("quantity");
			description.addProperty("price");
			description.addProperty("priceName");
			description.addProperty("lineItemOrder");
			description.addProperty("batchNumber");
			description.addProperty("status");
			return description;
		}
		return null;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("quantity");
		description.addProperty("priceUuid");
		description.addProperty("status");
		description.addProperty("lineItemOrder");
		description.addProperty("batchNumber");
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("status");
		description.addProperty("batchNumber");
		return description;
	}
	
	@PropertySetter(value = "status")
	public void setStatus(BillLineItem instance, Object status) {
		if (status == null) {
			return;
		}
		if (status instanceof BillLineItemStatus) {
			instance.setStatus((BillLineItemStatus) status);
		} else {
			instance.setStatus(BillLineItemStatus.valueOf(status.toString().trim().toUpperCase()));
		}
	}
	
	@PropertySetter(value = "item")
	public void setItem(BillLineItem instance, Object item) {
		StockManagementService service = Context.getService(StockManagementService.class);
		String itemUuid = (String) item;
		instance.setItem(service.getStockItemByUuid(itemUuid));
	}
	
	@PropertySetter(value = "billableService")
	public void setBillableService(BillLineItem instance, Object item) {
		BillableServiceService service = Context.getService(BillableServiceService.class);
		String serviceUuid = (String) item;
		instance.setBillableService(service.getBillableServiceByUuid(serviceUuid));
	}
	
	@PropertySetter(value = "billableDrug")
	public void setBillableDrug(BillLineItem instance, Object item) {
		BillableDrugService service = Context.getService(BillableDrugService.class);
		String drugUuid = (String) item;
		instance.setBillableDrug(service.getBillableDrugByUuid(drugUuid));
	}
	
	@PropertyGetter(value = "item")
	public String getItem(BillLineItem instance) {
		try {
			StockItem stockItem = instance.getItem();
			return stockItem.getDrug().getName();
		}
		catch (Exception e) {
			return "";
		}
	}
	
	@PropertyGetter(value = "billableService")
	public String getBillableService(BillLineItem instance) {
		try {
			BillableService service = instance.getBillableService();
			return service.getName();
		}
		catch (Exception e) {
			return "";
		}
	}
	
	@PropertyGetter(value = "billableDrug")
	public String getBillableDrug(BillLineItem instance) {
		try {
			BillableDrug drug = instance.getBillableDrug();
			return drug.getName();
		}
		catch (Exception e) {
			return "";
		}
	}
	
	@PropertyGetter(value = "priceName")
	public String getPriceName(BillLineItem instance) {
		String itemName = instance.getPriceName();
		return StringUtils.isNotBlank(itemName) ? itemName : "";
	}
	
	/**
	 * Write-only create input: snapshots amount, name, and catalog owner from CashierItemPrice. Does
	 * not persist a live {@code itemPrice} link.
	 */
	@PropertySetter(value = "priceUuid")
	public void setItemPrice(BillLineItem instance, String uuid) {
		BillLineItemPriceSnapshot.applyFromPriceUuid(instance, uuid);
		if (instance.getStatus() == null) {
			instance.setStatus(BillLineItemStatus.PENDING);
		}
	}
	
	@Override
	public BillLineItem getByUniqueId(String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return null;
		}
		
		return Context.getService(BillLineItemService.class).getBillLineItemByUuid(uuid);
	}
	
	@Override
	public BillLineItem newDelegate() {
		return new BillLineItem();
	}
	
	/**
	 * Persists status / batchNumber updates via the parent bill. Does not change bill status — marking
	 * the bill {@code PAID} remains a manual bill update so more line items can still be added.
	 */
	@Override
	public BillLineItem save(BillLineItem delegate) {
		if (delegate == null) {
			throw new APIException("Bill line item is required");
		}
		if (delegate.getId() == null) {
			throw new APIException("Creating bill line items via /billLineItem is not supported; "
			        + "use POST /bill/{uuid}/lineItem or nest lineItems on bill create");
		}
		Bill bill = delegate.getBill();
		if (bill == null) {
			throw new APIException("Bill line item is not attached to a bill");
		}
		if (Boolean.TRUE.equals(delegate.getVoided())) {
			throw new APIException("Cannot update a voided bill line item");
		}
		
		Context.getService(BillService.class).saveBill(bill);
		return delegate;
	}
	
	@Override
	public Class<IEntityDataService<BillLineItem>> getServiceClass() {
		// BillLineItemService doesn't implement IEntityDataService, so return null
		// Line items are managed through BillService, not directly
		return null;
	}
	
	@Override
	public void delete(BillLineItem lineItem, String reason, RequestContext context) {
		if (StringUtils.isBlank(reason)) {
			throw new IllegalArgumentException("Reason is required");
		}
		
		lineItem.setVoided(true);
		lineItem.setVoidReason(reason);
		lineItem.setVoidedBy(Context.getAuthenticatedUser());
		
		Context.getService(BillService.class).saveBill(lineItem.getBill());
	}
}
