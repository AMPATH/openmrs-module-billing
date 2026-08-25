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

import java.util.ArrayList;
import java.util.List;

import org.openmrs.BaseChangeableOpenmrsMetadata;
import org.openmrs.Drug;
import org.openmrs.Location;

/**
 * Catalog entry for a billable drug formulation, modeled after {@link BillableService}. Keyed by
 * OpenMRS {@link Drug} (formulation UUID) rather than concept.
 */
public class BillableDrug extends BaseChangeableOpenmrsMetadata {
	
	private static final long serialVersionUID = 0L;
	
	private int billableDrugId;
	
	private String name;
	
	private String shortName;
	
	private Drug drug;
	
	private Location location;
	
	private List<CashierItemPrice> drugPrices;
	
	private BillableDrugStatus status = BillableDrugStatus.ENABLED;
	
	public int getBillableDrugId() {
		return billableDrugId;
	}
	
	public void setBillableDrugId(int billableDrugId) {
		this.billableDrugId = billableDrugId;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	
	public Drug getDrug() {
		return drug;
	}
	
	public void setDrug(Drug drug) {
		this.drug = drug;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public BillableDrugStatus getStatus() {
		return status;
	}
	
	public void setStatus(BillableDrugStatus status) {
		this.status = status;
	}
	
	@Override
	public Integer getId() {
		return getBillableDrugId();
	}
	
	@Override
	public void setId(Integer id) {
		setBillableDrugId(id);
	}
	
	public List<CashierItemPrice> getDrugPrices() {
		return drugPrices;
	}
	
	public void setDrugPrices(List<CashierItemPrice> drugPrices) {
		this.drugPrices = drugPrices;
	}
	
	public void addDrugPrice(CashierItemPrice price) {
		if (price == null) {
			throw new NullPointerException("Drug price must be defined.");
		}
		if (this.drugPrices == null) {
			this.drugPrices = new ArrayList<>();
		}
		this.drugPrices.add(price);
		price.setBillableDrug(this);
	}
}
