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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.LocationBillableServiceService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.LocationBillableService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;

/**
 * Unit tests for {@link LocationBillableServiceResource} following the module's REST test pattern.
 */
public class LocationBillableServiceResourceTest {
	
	private LocationBillableServiceResource resource;
	
	private LocationBillableServiceService mappingService;
	
	private MockedStatic<Context> contextMock;
	
	@BeforeEach
	public void setUp() {
		resource = new LocationBillableServiceResource();
		mappingService = mock(LocationBillableServiceService.class);
		contextMock = mockStatic(Context.class);
		contextMock.when(() -> Context.getService(LocationBillableServiceService.class)).thenReturn(mappingService);
	}
	
	@AfterEach
	public void tearDown() {
		contextMock.close();
	}
	
	private RequestContext requestContext(String... params) {
		HttpServletRequest request = mock(HttpServletRequest.class);
		for (int i = 0; i + 1 < params.length; i += 2) {
			when(request.getParameter(params[i])).thenReturn(params[i + 1]);
		}
		RequestContext context = mock(RequestContext.class);
		when(context.getRequest()).thenReturn(request);
		when(context.getStartIndex()).thenReturn(0);
		when(context.getLimit()).thenReturn(10);
		return context;
	}
	
	@Test
	public void getRepresentationDescription_shouldIncludeLocationScopedProperties() {
		DelegatingResourceDescription description = resource.getRepresentationDescription(
		    new org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation());
		assertNotNull(description);
		assertNotNull(description.getProperties().get("location"));
		assertNotNull(description.getProperties().get("priceOverride"));
		assertNotNull(description.getProperties().get("enabled"));
		assertNotNull(description.getProperties().get("displayOrder"));
	}
	
	@Test
	public void getCreatableProperties_shouldRequireLocationAndService() {
		DelegatingResourceDescription description = resource.getCreatableProperties();
		assertNotNull(description.getProperties().get("location"));
		assertNotNull(description.getProperties().get("billableService"));
	}
	
	@Test
	public void doSearch_shouldPassLocationGroupAndServiceFiltersToService() {
		String locationUuid = "11111111-aaaa-2222-bbbb-333333333301";
		String groupUuid = "group-uuid";
		String serviceUuid = "service-uuid";
		
		Location location = new Location();
		LocationService locationService = mock(LocationService.class);
		contextMock.when(Context::getLocationService).thenReturn(locationService);
		when(locationService.getLocationByUuid(locationUuid)).thenReturn(location);
		
		BillableService billableService = new BillableService();
		BillableServiceService billableServiceService = mock(BillableServiceService.class);
		contextMock.when(() -> Context.getService(BillableServiceService.class)).thenReturn(billableServiceService);
		when(billableServiceService.getBillableServiceByUuid(serviceUuid)).thenReturn(billableService);
		
		when(mappingService.getBillableServicesByLocation(any(), any(), any(), anyBoolean(), any(PagingInfo.class)))
		        .thenReturn(Collections.emptyList());
		
		resource.doSearch(requestContext("location", locationUuid, "group", groupUuid, "service", serviceUuid));
		
		verify(mappingService).getBillableServicesByLocation(eq(location), any(), eq(billableService), eq(false),
		    any(PagingInfo.class));
	}
	
	@Test
	public void doSearch_shouldRejectUnknownLocation() {
		LocationService locationService = mock(LocationService.class);
		contextMock.when(Context::getLocationService).thenReturn(locationService);
		when(locationService.getLocationByUuid("bogus")).thenReturn(null);
		
		assertThrows(IllegalArgumentException.class, () -> resource.doSearch(requestContext("location", "bogus")));
	}
	
	@Test
	public void save_shouldDelegateToService() {
		LocationBillableService delegate = new LocationBillableService();
		delegate.setPriceOverride(BigDecimal.TEN);
		when(mappingService.saveLocationBillableService(delegate)).thenReturn(delegate);
		
		assertEquals(delegate, resource.save(delegate));
	}
	
	@Test
	public void delete_shouldVoidWithReason() throws ResourceDoesNotSupportOperationException {
		LocationBillableService delegate = new LocationBillableService();
		
		org.openmrs.module.webservices.rest.web.RequestContext ctx = requestContext();
		resource.delete(delegate, "not needed anymore", ctx);
		
		verify(mappingService).voidLocationBillableService(delegate, "not needed anymore");
	}
}
