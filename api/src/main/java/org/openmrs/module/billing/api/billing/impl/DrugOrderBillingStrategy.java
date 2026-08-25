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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.DrugOrder;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.module.billing.api.BillableDrugService;
import org.openmrs.module.billing.api.ItemPriceService;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillLineItemStatus;
import org.openmrs.module.billing.api.model.BillableDrug;
import org.openmrs.module.billing.api.model.BillableDrugStatus;
import org.openmrs.module.billing.api.model.CashPoint;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.api.model.ExemptionType;
import org.openmrs.module.billing.api.search.BillableDrugSearch;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Billing strategy for {@link DrugOrder}s. Prefers {@link BillableDrug} by drug UUID (with location
 * fallback); falls back to stockmanagement {@link StockItem} for backward compatibility.
 */
@Slf4j
@Setter(onMethod_ = @Autowired)
public class DrugOrderBillingStrategy extends AbstractDefaultOrderBillingStrategy {
	
	private StockManagementService stockManagementService;
	
	private ItemPriceService itemPriceService;
	
	private BillableDrugService billableDrugService;
	
	@Override
	protected boolean supportsOrder(Order order) {
		return order instanceof DrugOrder;
	}
	
	@Override
	protected Optional<BillLineItem> createBillLineItem(Order order) {
		DrugOrder drugOrder = (DrugOrder) order;
		
		if (drugOrder.getDrug() == null) {
			log.warn("DrugOrder {} has no drug set, cannot generate bill", order.getUuid());
			return Optional.empty();
		}
		
		Location location = resolveEncounterLocation(order);
		Optional<BillLineItem> fromBillableDrug = createFromBillableDrug(drugOrder, location);
		if (fromBillableDrug.isPresent()) {
			return fromBillableDrug;
		}
		
		return createFromStockItem(drugOrder);
	}
	
	private Optional<BillLineItem> createFromBillableDrug(DrugOrder drugOrder, Location location) {
		BillableDrugSearch search = new BillableDrugSearch();
		search.setDrugUuid(drugOrder.getDrug().getUuid());
		search.setStatus(BillableDrugStatus.ENABLED);
		if (location != null) {
			search.setLocationUuid(location.getUuid());
			search.setIncludeGlobal(true);
		}
		
		List<BillableDrug> matches = billableDrugService.getBillableDrugs(search, null);
		BillableDrug billableDrug = preferLocationSpecific(matches, location);
		if (billableDrug == null) {
			return Optional.empty();
		}
		
		int quantity = (int) (drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0);
		boolean isExempted = checkIfOrderIsExempted(drugOrder, ExemptionType.COMMODITY);
		BillLineItemStatus lineItemStatus = isExempted ? BillLineItemStatus.EXEMPTED : BillLineItemStatus.PENDING;
		
		BigDecimal price = resolveDrugPrice(billableDrug);
		BillLineItem lineItem = createLineItem(price, quantity, lineItemStatus, drugOrder);
		lineItem.setBillableDrug(billableDrug);
		List<CashierItemPrice> prices = itemPriceService.getDrugPrice(billableDrug);
		if (!prices.isEmpty()) {
			lineItem.setItemPrice(prices.get(0));
		}
		return Optional.of(lineItem);
	}
	
	private BillableDrug preferLocationSpecific(List<BillableDrug> matches, Location location) {
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		if (location != null) {
			for (BillableDrug drug : matches) {
				if (drug.getLocation() != null && location.equals(drug.getLocation())) {
					return drug;
				}
			}
		}
		for (BillableDrug drug : matches) {
			if (drug.getLocation() == null) {
				return drug;
			}
		}
		return matches.get(0);
	}
	
	private BigDecimal resolveDrugPrice(BillableDrug billableDrug) {
		List<CashierItemPrice> itemPrices = itemPriceService.getDrugPrice(billableDrug);
		if (!itemPrices.isEmpty()) {
			return itemPrices.get(0).getPrice();
		}
		return BigDecimal.ZERO;
	}
	
	private Optional<BillLineItem> createFromStockItem(DrugOrder drugOrder) {
		Integer drugId = drugOrder.getDrug().getDrugId();
		List<StockItem> stockItems = stockManagementService.getStockItemByDrug(drugId);
		
		if (stockItems.isEmpty()) {
			log.debug("No BillableDrug or stock item found for drug ID: {}", drugId);
			return Optional.empty();
		}
		
		int quantity = (int) (drugOrder.getQuantity() != null ? drugOrder.getQuantity() : 0.0);
		boolean isExempted = checkIfOrderIsExempted(drugOrder, ExemptionType.COMMODITY);
		BillLineItemStatus lineItemStatus = isExempted ? BillLineItemStatus.EXEMPTED : BillLineItemStatus.PENDING;
		
		StockItem stockItem = preferPricedStockItem(stockItems);
		BillLineItem lineItem = createLineItem(resolveStockPrice(stockItem), quantity, lineItemStatus, drugOrder);
		lineItem.setItem(stockItem);
		return Optional.of(lineItem);
	}
	
	private StockItem preferPricedStockItem(List<StockItem> stockItems) {
		for (StockItem stockItem : stockItems) {
			if (!itemPriceService.getItemPrice(stockItem).isEmpty()) {
				return stockItem;
			}
		}
		return stockItems.get(0);
	}
	
	private BigDecimal resolveStockPrice(StockItem stockItem) {
		List<CashierItemPrice> itemPrices = itemPriceService.getItemPrice(stockItem);
		if (!itemPrices.isEmpty()) {
			return itemPrices.get(0).getPrice();
		} else if (stockItem.getPurchasePrice() != null) {
			return stockItem.getPurchasePrice();
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
