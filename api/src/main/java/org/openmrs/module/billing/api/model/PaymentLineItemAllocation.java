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

import lombok.Getter;
import lombok.Setter;
import org.openmrs.BaseChangeableOpenmrsData;

/**
 * Allocates a portion of a {@link Payment} to a specific {@link BillLineItem}.
 */
@Getter
@Setter
public class PaymentLineItemAllocation extends BaseChangeableOpenmrsData {
	
	private static final long serialVersionUID = 0L;
	
	private Integer paymentLineItemId;
	
	private Payment payment;
	
	private BillLineItem billLineItem;
	
	private BigDecimal amount;
	
	@Override
	public Integer getId() {
		return paymentLineItemId;
	}
	
	@Override
	public void setId(Integer id) {
		this.paymentLineItemId = id;
	}
}
