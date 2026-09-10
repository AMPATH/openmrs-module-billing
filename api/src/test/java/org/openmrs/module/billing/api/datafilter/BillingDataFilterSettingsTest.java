/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.datafilter;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.AdministrationDAO;

/**
 * Tests {@link BillingDataFilterSettings}, mostly to pin down that billing data is only scoped by
 * location once an administrator has asked for it.
 */
@ExtendWith(MockitoExtension.class)
public class BillingDataFilterSettingsTest {
	
	private AdministrationDAO adminDAO;
	
	private MockedStatic<Context> contextMock;
	
	@BeforeEach
	public void setUp() {
		adminDAO = mock(AdministrationDAO.class);
		contextMock = mockStatic(Context.class);
		contextMock.when(() -> Context.getRegisteredComponent("adminDAO", AdministrationDAO.class)).thenReturn(adminDAO);
	}
	
	@AfterEach
	public void tearDown() {
		contextMock.close();
	}
	
	@Test
	public void isScopingEnabled_shouldBeFalseWhenThePropertyHasNeverBeenSet() {
		//How an existing installation upgrades: no row at all
		stubRows(Collections.emptyList());
		
		assertFalse(BillingDataFilterSettings.isScopingEnabled());
	}
	
	@Test
	public void isScopingEnabled_shouldBeFalseWhenThePropertyIsEmpty() {
		stubRows(rows(""));
		
		assertFalse(BillingDataFilterSettings.isScopingEnabled());
	}
	
	@Test
	public void isScopingEnabled_shouldBeFalseWhenThePropertyIsNull() {
		stubRows(rows((Object) null));
		
		assertFalse(BillingDataFilterSettings.isScopingEnabled());
	}
	
	@Test
	public void isScopingEnabled_shouldBeFalseWhenThePropertyIsFalse() {
		stubRows(rows("false"));
		
		assertFalse(BillingDataFilterSettings.isScopingEnabled());
	}
	
	@Test
	public void isScopingEnabled_shouldBeTrueWhenThePropertyIsTrue() {
		stubRows(rows("true"));
		
		assertTrue(BillingDataFilterSettings.isScopingEnabled());
	}
	
	@Test
	public void isScopingEnabled_shouldIgnoreTheCaseAndSurroundingWhitespaceOfTheProperty() {
		stubRows(rows(" TRUE "));
		
		assertTrue(BillingDataFilterSettings.isScopingEnabled());
	}
	
	private void stubRows(List<List<Object>> rows) {
		when(adminDAO.executeSQL(BillingDataFilterConstants.ENABLED_QUERY, true)).thenReturn(rows);
	}
	
	private static List<List<Object>> rows(Object... values) {
		return Collections.singletonList(asList(values));
	}
	
}
