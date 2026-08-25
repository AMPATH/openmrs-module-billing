/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.module.billing.api.model.BillableDrugStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillableDrugSearch {
	
	private BillableDrugStatus status;
	
	private String drugUuid;
	
	private String locationUuid;
	
	private String name;
	
	private Boolean includeRetired = false;
	
	/**
	 * When true and {@link #locationUuid} is set, also include billable drugs with null location
	 * (global catalog entries).
	 */
	private Boolean includeGlobal = false;
}
