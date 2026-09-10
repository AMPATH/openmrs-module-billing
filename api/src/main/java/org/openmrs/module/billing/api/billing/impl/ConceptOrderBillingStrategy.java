/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.billing.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.DrugOrder;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.TestOrder;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.ItemPriceService;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceStatus;
import org.openmrs.module.billing.api.model.CashPoint;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.api.model.ExemptionType;
import org.openmrs.module.billing.api.search.BillableServiceSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

/**
 * Bills non-drug, non-test orders by mapping the order concept to a location-scoped
 * {@link BillableService}.
 */
@Slf4j
@Setter(onMethod_ = @Autowired)
public class ConceptOrderBillingStrategy extends AbstractDefaultOrderBillingStrategy {
	
	private BillableServiceService billableServiceService;
	
	private ItemPriceService itemPriceService;
	
	@Override
	protected boolean supportsOrder(Order order) {
		return !(order instanceof DrugOrder) && !(order instanceof TestOrder) && order.getConcept() != null;
	}
	
	@Override
	public int getOrder() {
		// After drug and test strategies
		return Ordered.LOWEST_PRECEDENCE;
	}
	
	@Override
	protected Optional<BillLineItem> createBillLineItem(Order order) {
		Location location = resolveEncounterLocation(order);
		BillableServiceSearch searchTemplate = new BillableServiceSearch();
		searchTemplate.setConceptUuid(order.getConcept().getUuid());
		searchTemplate.setServiceStatus(BillableServiceStatus.ENABLED);
		if (location != null) {
			searchTemplate.setLocationUuid(location.getUuid());
			searchTemplate.setIncludeGlobal(true);
		}
		
		List<BillableService> searchResult = billableServiceService.getBillableServices(searchTemplate, null);
		BillableService billableService = preferLocationSpecific(searchResult, location);
		if (billableService == null) {
			log.debug("No billable service found for concept: {}", order.getConcept().getUuid());
			return Optional.empty();
		}
		
		BillLineItemStatus lineItemStatus = checkIfOrderIsExempted(order, ExemptionType.SERVICE)
		        ? BillLineItemStatus.EXEMPTED
		        : BillLineItemStatus.PENDING;
		
		BillLineItem lineItem = createLineItem(resolvePrice(billableService), 1, lineItemStatus, order);
		lineItem.setBillableService(billableService);
		return Optional.of(lineItem);
	}
	
	private BillableService preferLocationSpecific(List<BillableService> matches, Location location) {
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		if (location != null) {
			for (BillableService service : matches) {
				if (service.getLocation() != null && location.equals(service.getLocation())) {
					return service;
				}
			}
		}
		for (BillableService service : matches) {
			if (service.getLocation() == null) {
				return service;
			}
		}
		return matches.get(0);
	}
	
	private BigDecimal resolvePrice(BillableService billableService) {
		List<CashierItemPrice> itemPrices = itemPriceService.getServicePrice(billableService);
		if (!itemPrices.isEmpty()) {
			return itemPrices.get(0).getPrice();
		}
		return BigDecimal.ZERO;
	}
	
	@Override
	public Provider resolveCashier(Order order) {
		return order.getOrderer();
	}
	
	@Override
	public CashPoint resolveCashPoint(Order order) {
		return resolveCashPointForOrder(order);
	}
}
