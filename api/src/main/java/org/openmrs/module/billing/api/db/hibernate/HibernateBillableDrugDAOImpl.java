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

import static org.openmrs.module.billing.api.db.hibernate.PagingUtil.applyPaging;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Location;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.db.BillableDrugDAO;
import org.openmrs.module.billing.api.model.BillableDrug;
import org.openmrs.module.billing.api.search.BillableDrugSearch;

@RequiredArgsConstructor
public class HibernateBillableDrugDAOImpl implements BillableDrugDAO {
	
	private final SessionFactory sessionFactory;
	
	@Override
	public BillableDrug getBillableDrug(@Nonnull Integer id) {
		return sessionFactory.getCurrentSession().find(BillableDrug.class, id);
	}
	
	@Override
	public BillableDrug getBillableDrugByUuid(@Nonnull String uuid) {
		TypedQuery<BillableDrug> query = sessionFactory.getCurrentSession()
		        .createQuery("select b from BillableDrug b where b.uuid = :uuid", BillableDrug.class);
		query.setParameter("uuid", uuid);
		return query.getResultStream().findFirst().orElse(null);
	}
	
	@Override
	public List<BillableDrug> getBillableDrugs(@Nonnull BillableDrugSearch search, PagingInfo pagingInfo) {
		Session session = sessionFactory.getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<BillableDrug> cq = cb.createQuery(BillableDrug.class);
		Root<BillableDrug> root = cq.from(BillableDrug.class);
		
		List<Predicate> predicates = buildSearchPredicates(cb, root, search);
		if (!predicates.isEmpty()) {
			cq.where(predicates.toArray(new Predicate[0]));
		}
		
		TypedQuery<BillableDrug> query = session.createQuery(cq);
		applyPaging(query, pagingInfo, (countCb, countQuery, countRoot) -> buildSearchPredicates(countCb, countRoot, search),
		    sessionFactory, BillableDrug.class);
		return query.getResultList();
	}
	
	private List<Predicate> buildSearchPredicates(CriteriaBuilder cb, Root<BillableDrug> root, BillableDrugSearch search) {
		List<Predicate> predicates = new ArrayList<>();
		
		if (search.getStatus() != null) {
			predicates.add(cb.equal(root.get("status"), search.getStatus()));
		}
		if (StringUtils.isNotEmpty(search.getDrugUuid())) {
			predicates.add(cb.equal(root.get("drug").get("uuid"), search.getDrugUuid()));
		}
		if (StringUtils.isNotEmpty(search.getLocationUuid())) {
			// LEFT JOIN so global rows (location IS NULL) are not dropped by an implicit inner join
			Join<BillableDrug, Location> locationJoin = root.join("location", JoinType.LEFT);
			if (Boolean.TRUE.equals(search.getIncludeGlobal())) {
				predicates.add(cb.or(cb.equal(locationJoin.get("uuid"), search.getLocationUuid()), cb.isNull(locationJoin)));
			} else {
				predicates.add(cb.equal(locationJoin.get("uuid"), search.getLocationUuid()));
			}
		}
		if (StringUtils.isNotEmpty(search.getName())) {
			predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.getName().toLowerCase() + "%"));
		}
		if (!Boolean.TRUE.equals(search.getIncludeRetired())) {
			predicates.add(cb.equal(root.get("retired"), false));
		}
		return predicates;
	}
	
	@Override
	public BillableDrug saveBillableDrug(@Nonnull BillableDrug billableDrug) {
		sessionFactory.getCurrentSession().saveOrUpdate(billableDrug);
		return billableDrug;
	}
	
	@Override
	public void purgeBillableDrug(BillableDrug billableDrug) {
		sessionFactory.getCurrentSession().delete(billableDrug);
	}
}
