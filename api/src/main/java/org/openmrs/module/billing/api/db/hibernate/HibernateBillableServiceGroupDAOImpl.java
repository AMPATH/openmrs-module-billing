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

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.db.BillableServiceGroupDAO;
import org.openmrs.module.billing.api.model.BillableServiceGroup;

import javax.annotation.Nonnull;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.billing.api.db.hibernate.PagingUtil.applyPaging;

@RequiredArgsConstructor
public class HibernateBillableServiceGroupDAOImpl implements BillableServiceGroupDAO {
	
	private final SessionFactory sessionFactory;
	
	/** {@inheritDoc} */
	@Override
	public BillableServiceGroup getBillableServiceGroup(@Nonnull Integer id) {
		return sessionFactory.getCurrentSession().find(BillableServiceGroup.class, id);
	}
	
	/** {@inheritDoc} */
	@Override
	public BillableServiceGroup getBillableServiceGroupByUuid(@Nonnull String uuid) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from BillableServiceGroup where uuid = :uuid", BillableServiceGroup.class)
		        .setParameter("uuid", uuid).uniqueResultOptional().orElse(null);
	}
	
	/** {@inheritDoc} */
	@Override
	public List<BillableServiceGroup> getBillableServiceGroups(Location location, boolean includeRetired) {
		Session session = sessionFactory.getCurrentSession();
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<BillableServiceGroup> cq = cb.createQuery(BillableServiceGroup.class);
		Root<BillableServiceGroup> root = cq.from(BillableServiceGroup.class);
		
		List<Predicate> predicates = new ArrayList<>();
		if (location != null) {
			predicates.add(cb.equal(root.get("location"), location));
		}
		if (!includeRetired) {
			predicates.add(cb.equal(root.get("retired"), false));
		}
		
		if (!predicates.isEmpty()) {
			cq.where(predicates.toArray(new Predicate[0]));
		}
		cq.orderBy(cb.asc(root.get("name")));
		
		TypedQuery<BillableServiceGroup> query = session.createQuery(cq);
		return query.getResultList();
	}
	
	/** {@inheritDoc} */
	@Override
	public BillableServiceGroup saveBillableServiceGroup(@Nonnull BillableServiceGroup billableServiceGroup) {
		sessionFactory.getCurrentSession().saveOrUpdate(billableServiceGroup);
		return billableServiceGroup;
	}
	
	/** {@inheritDoc} */
	@Override
	public void purgeBillableServiceGroup(@Nonnull BillableServiceGroup billableServiceGroup) {
		sessionFactory.getCurrentSession().delete(billableServiceGroup);
	}
}
