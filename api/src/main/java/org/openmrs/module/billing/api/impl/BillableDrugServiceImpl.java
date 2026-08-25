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

import java.util.Collections;
import java.util.List;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.billing.api.BillableDrugService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.db.BillableDrugDAO;
import org.openmrs.module.billing.api.model.BillableDrug;
import org.openmrs.module.billing.api.search.BillableDrugSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class BillableDrugServiceImpl extends BaseOpenmrsService implements BillableDrugService {
	
	@Setter(onMethod_ = { @Autowired })
	private BillableDrugDAO billableDrugDAO;
	
	@Override
	@Transactional(readOnly = true)
	public BillableDrug getBillableDrug(Integer id) {
		if (id == null) {
			return null;
		}
		return billableDrugDAO.getBillableDrug(id);
	}
	
	@Override
	@Transactional(readOnly = true)
	public BillableDrug getBillableDrugByUuid(String uuid) {
		if (StringUtils.isEmpty(uuid)) {
			return null;
		}
		return billableDrugDAO.getBillableDrugByUuid(uuid);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<BillableDrug> getBillableDrugs(BillableDrugSearch search, PagingInfo pagingInfo) {
		if (search == null) {
			return Collections.emptyList();
		}
		return billableDrugDAO.getBillableDrugs(search, pagingInfo);
	}
	
	@Override
	@Transactional
	public BillableDrug saveBillableDrug(BillableDrug billableDrug) {
		if (billableDrug == null) {
			throw new NullPointerException("The billableDrug must be defined.");
		}
		return billableDrugDAO.saveBillableDrug(billableDrug);
	}
	
	@Override
	@Transactional
	public void purgeBillableDrug(BillableDrug billableDrug) {
		if (billableDrug == null) {
			throw new NullPointerException("The billableDrug must be defined.");
		}
		billableDrugDAO.purgeBillableDrug(billableDrug);
	}
	
	@Override
	@Transactional
	public BillableDrug retireBillableDrug(BillableDrug billableDrug, String reason) {
		if (StringUtils.isEmpty(reason)) {
			throw new IllegalArgumentException("Retire reason cannot be empty or null");
		}
		return billableDrugDAO.saveBillableDrug(billableDrug);
	}
	
	@Override
	@Transactional
	public BillableDrug unretireBillableDrug(BillableDrug billableDrug) {
		return billableDrugDAO.saveBillableDrug(billableDrug);
	}
}
