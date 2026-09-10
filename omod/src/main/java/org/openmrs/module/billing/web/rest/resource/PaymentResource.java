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
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.base.ProviderUtil;
import org.openmrs.module.billing.api.PaymentModeService;
import org.openmrs.module.billing.web.base.resource.BaseRestDataResource;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.model.Payment;
import org.openmrs.module.billing.api.model.PaymentAttribute;
import org.openmrs.module.billing.api.model.PaymentLineItemAllocation;
import org.openmrs.module.billing.api.model.PaymentMode;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST resource representing a {@link Payment}.
 */
@SubResource(parent = BillResource.class, path = "payment", supportedClass = Payment.class, supportedOpenmrsVersions = {
        "2.0 - 2.*" })
public class PaymentResource extends DelegatingSubResource<Payment, Bill, BillResource> {
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("uuid");
			description.addProperty("instanceType", Representation.REF);
			description.addProperty("attributes");
			description.addProperty("amount");
			description.addProperty("amountTendered");
			description.addProperty("cashier", Representation.REF);
			description.addProperty("dateCreated");
			description.addProperty("voided");
			description.addProperty("lineItems");
			return description;
		}
		
		return null;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("instanceType");
		description.addProperty("attributes");
		description.addProperty("amount");
		description.addProperty("amountTendered");
		description.addProperty("cashier");
		description.addProperty("lineItems");
		
		return description;
	}
	
	@PropertySetter("cashier")
	public void setCashier(Payment instance, String uuid) {
		if (StringUtils.isBlank(uuid)) {
			throw new APIException("Cashier UUID must not be null or blank.");
		}
		Provider provider = Context.getProviderService().getProviderByUuid(uuid);
		if (provider == null) {
			throw new ObjectNotFoundException();
		}
		instance.setCashier(provider);
	}
	
	// Work around TypeVariable issue on base generic property (BaseCustomizableInstanceData.getInstanceType)
	@PropertySetter("instanceType")
	public void setPaymentMode(Payment instance, String uuid) {
		PaymentModeService service = Context.getService(PaymentModeService.class);
		
		PaymentMode mode = service.getPaymentModeByUuid(uuid);
		if (mode == null) {
			throw new ObjectNotFoundException();
		}
		
		instance.setInstanceType(mode);
	}
	
	@PropertySetter("attributes")
	public void setPaymentAttributes(Payment instance, Set<PaymentAttribute> attributes) {
		if (instance.getAttributes() == null) {
			instance.setAttributes(new HashSet<PaymentAttribute>());
		}
		
		BaseRestDataResource.syncCollection(instance.getAttributes(), attributes);
		for (PaymentAttribute attr : instance.getAttributes()) {
			attr.setOwner(instance);
		}
	}
	
	@PropertySetter("amount")
	public void setPaymentAmount(Payment instance, Object price) {
		// TODO Conversion logic
		double amount;
		if (price instanceof Integer) {
			int rawAmount = (Integer) price;
			amount = Double.valueOf(rawAmount);
			instance.setAmount(BigDecimal.valueOf(amount));
		} else {
			instance.setAmount(BigDecimal.valueOf((Double) price));
		}
	}
	
	@PropertySetter("amountTendered")
	public void setPaymentAmountTendered(Payment instance, Object price) {
		// TODO Conversion logic
		double amount;
		if (price instanceof Integer) {
			int rawAmount = (Integer) price;
			amount = Double.valueOf(rawAmount);
			instance.setAmountTendered(BigDecimal.valueOf(amount));
		} else {
			instance.setAmountTendered(BigDecimal.valueOf((Double) price));
		}
	}
	
	@PropertyGetter("lineItems")
	public List<Map<String, Object>> getLineItems(Payment instance) {
		List<Map<String, Object>> result = new ArrayList<>();
		if (instance.getLineItemAllocations() == null) {
			return result;
		}
		for (PaymentLineItemAllocation allocation : instance.getLineItemAllocations()) {
			if (allocation == null || allocation.getVoided()) {
				continue;
			}
			Map<String, Object> row = new HashMap<>();
			if (allocation.getBillLineItem() != null) {
				row.put("uuid", allocation.getBillLineItem().getUuid());
			}
			row.put("amount", allocation.getAmount());
			result.add(row);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@PropertySetter("lineItems")
	public void setLineItems(Payment instance, Object lineItemsObj) {
		if (lineItemsObj == null) {
			return;
		}
		if (!(lineItemsObj instanceof List)) {
			throw new APIException("lineItems must be a list of { uuid, amount } objects");
		}
		List<?> lineItems = (List<?>) lineItemsObj;
		Bill bill = instance.getBill();
		BigDecimal allocationSum = BigDecimal.ZERO;
		for (Object entry : lineItems) {
			Map<String, Object> map;
			if (entry instanceof Map) {
				map = (Map<String, Object>) entry;
			} else {
				throw new APIException("Each lineItems entry must be a map with uuid and amount");
			}
			String lineUuid = map.get("uuid") != null ? map.get("uuid").toString() : null;
			if (StringUtils.isBlank(lineUuid)) {
				throw new APIException("lineItems entry requires uuid");
			}
			BillLineItem lineItem = findLineItem(bill, lineUuid);
			if (lineItem.getStatus() == BillLineItemStatus.PAID || lineItem.getStatus() == BillLineItemStatus.EXEMPTED) {
				throw new APIException("Cannot allocate payment to line item in status " + lineItem.getStatus());
			}
			if (bill != null && (lineItem.getBill() == null || !bill.equals(lineItem.getBill())
			        && (bill.getUuid() == null || !bill.getUuid().equals(lineItem.getBill().getUuid())))) {
				throw new APIException("lineItems uuid does not belong to this bill");
			}
			BigDecimal amount = toBigDecimal(map.get("amount"));
			if (amount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new APIException("lineItems amount must be positive");
			}
			allocationSum = allocationSum.add(amount);
			PaymentLineItemAllocation allocation = new PaymentLineItemAllocation();
			allocation.setBillLineItem(lineItem);
			allocation.setAmount(amount);
			instance.addLineItemAllocation(allocation);
		}
		if (instance.getAmountTendered() != null && allocationSum.compareTo(instance.getAmountTendered()) > 0) {
			throw new APIException("Sum of lineItems amounts cannot exceed amountTendered");
		}
	}
	
	private BillLineItem findLineItem(Bill bill, String lineUuid) {
		if (bill != null && bill.getLineItems() != null) {
			for (BillLineItem lineItem : bill.getLineItems()) {
				if (lineItem != null && lineUuid.equals(lineItem.getUuid())) {
					return lineItem;
				}
			}
		}
		BillLineItem fromService = Context.getService(org.openmrs.module.billing.api.BillLineItemService.class)
		        .getBillLineItemByUuid(lineUuid);
		if (fromService != null) {
			return fromService;
		}
		throw new ObjectNotFoundException();
	}
	
	private BigDecimal toBigDecimal(Object amount) {
		if (amount == null) {
			throw new APIException("lineItems amount is required");
		}
		if (amount instanceof BigDecimal) {
			return (BigDecimal) amount;
		}
		if (amount instanceof Integer) {
			return BigDecimal.valueOf((Integer) amount);
		}
		if (amount instanceof Number) {
			return BigDecimal.valueOf(((Number) amount).doubleValue());
		}
		return new BigDecimal(amount.toString());
	}
	
	@PropertyGetter("dateCreated")
	public Long getPaymentDate(Payment instance) {
		return instance.getDateCreated().getTime();
	}
	
	@Override
	public Payment save(Payment delegate) {
		if (delegate.getCashier() == null) {
			Provider cashier = ProviderUtil.getCurrentProvider();
			if (cashier == null) {
				throw new APIException(
				        "The authenticated user is not associated with a Provider and cannot process payments.");
			}
			delegate.setCashier(cashier);
		}
		
		BillService service = Context.getService(BillService.class);
		Bill bill = delegate.getBill();
		bill.addPayment(delegate);
		service.saveBill(bill);
		
		return delegate;
	}
	
	@Override
	protected void delete(Payment delegate, String reason, RequestContext context) {
		delete(delegate.getBill().getUuid(), delegate.getUuid(), reason, context);
	}
	
	@Override
	public void delete(String parentUniqueId, final String uuid, String reason, RequestContext context) {
		BillService service = Context.getService(BillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);
		
		payment.setVoided(true);
		payment.setVoidReason(reason);
		payment.setVoidedBy(Context.getAuthenticatedUser());
		
		bill.synchronizeBillStatus();
		service.saveBill(bill);
	}
	
	@Override
	public void purge(Payment delegate, RequestContext context) {
		purge(delegate.getBill().getUuid(), delegate.getUuid(), context);
	}
	
	@Override
	public void purge(String parentUniqueId, String uuid, RequestContext context) {
		BillService service = Context.getService(BillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);
		
		bill.removePayment(payment);
		service.saveBill(bill);
	}
	
	@Override
	public PageableResult doGetAll(Bill parent, RequestContext context) {
		return new AlreadyPaged<Payment>(context, new ArrayList<Payment>(parent.getPayments()), false);
	}
	
	@Override
	public Payment getByUniqueId(String uniqueId) {
		return null;
	}
	
	@Override
	public Bill getParent(Payment instance) {
		return instance.getBill();
	}
	
	@Override
	public void setParent(Payment instance, Bill parent) {
		instance.setBill(parent);
	}
	
	@Override
	public Payment newDelegate() {
		return new Payment();
	}
	
	private Bill findBill(BillService service, String billUUID) {
		Bill bill = service.getBillByUuid(billUUID);
		if (bill == null) {
			throw new ObjectNotFoundException();
		}
		
		return bill;
	}
	
	private Payment findPayment(Bill bill, final String paymentUUID) {
		
		for (Payment payment : bill.getPayments()) {
			if (payment != null && payment.getUuid().equals(paymentUUID)) {
				return payment;
			}
		}
		throw new ObjectNotFoundException();
	}
}
