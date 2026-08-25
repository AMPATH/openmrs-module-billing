/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.db.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.db.LocationBillableServiceDAO;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.BillableServiceGroup;
import org.openmrs.module.billing.api.model.LocationBillableService;
import org.openmrs.module.billing.api.util.BillingLocationScope;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.billing.api.db.hibernate.PagingUtil.applyPaging;

public class HibernateLocationBillableServiceDAOImpl implements LocationBillableServiceDAO {
	
	private final SessionFactory sessionFactory;
	
	@Autowired(required = false)
	private BillingLocationScope locationScope;
	
	@Autowired(required = false)
	public void setLocationScope(BillingLocationScope locationScope) {
		this.locationScope = locationScope;
	}
	
	public HibernateLocationBillableServiceDAOImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/** {@inheritDoc} */
	@Override
	public LocationBillableService getLocationBillableService(@Nonnull Integer id) {
		return sessionFactory.getCurrentSession().find(LocationBillableService.class, id);
	}
	
	/** {@inheritDoc} */
	@Override
	public LocationBillableService getLocationBillableServiceByUuid(@Nonnull String uuid) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from LocationBillableService where uuid = :uuid", LocationBillableService.class)
		        .setParameter("uuid", uuid).uniqueResultOptional().orElse(null);
	}
	
	/** {@inheritDoc} */
	@Override
	public List<LocationBillableService> getLocationBillableServices(Location location, BillableServiceGroup serviceGroup,
	        BillableService billableService, boolean includeRetired, PagingInfo pagingInfo) {
		if (locationScope != null) {
			locationScope.apply();
		}
		Session session = sessionFactory.getCurrentSession();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<LocationBillableService> cq = cb.createQuery(LocationBillableService.class);
		Root<LocationBillableService> root = cq.from(LocationBillableService.class);
		
		List<Predicate> predicates = new ArrayList<>();
		if (location != null) {
			predicates.add(cb.equal(root.get("location"), location));
		}
		if (serviceGroup != null) {
			predicates.add(cb.equal(root.get("serviceGroup"), serviceGroup));
		}
		if (billableService != null) {
			predicates.add(cb.equal(root.get("billableService"), billableService));
		}
		if (!includeRetired) {
			predicates.add(cb.equal(root.get("voided"), false));
		}
		
		if (!predicates.isEmpty()) {
			cq.where(predicates.toArray(new Predicate[0]));
		}
		cq.orderBy(cb.asc(root.get("displayOrder")), cb.asc(root.get("id")));
		
		TypedQuery<LocationBillableService> query = session.createQuery(cq);
		
		applyPaging(query, pagingInfo, predicates, sessionFactory, LocationBillableService.class);
		
		return query.getResultList();
	}
	
	/** {@inheritDoc} */
	@Override
	public LocationBillableService saveLocationBillableService(@Nonnull LocationBillableService locationBillableService) {
		sessionFactory.getCurrentSession().saveOrUpdate(locationBillableService);
		return locationBillableService;
	}
	
	/** {@inheritDoc} */
	@Override
	public void purgeLocationBillableService(@Nonnull LocationBillableService locationBillableService) {
		sessionFactory.getCurrentSession().delete(locationBillableService);
	}
}
