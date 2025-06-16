/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.internal.graphql.query.v1_0;

import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.petra.function.UnsafeFunction;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.vulcan.accept.language.AcceptLanguage;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.trash.rest.dto.v1_0.RecycleBinEntry;
import com.liferay.trash.rest.resource.v1_0.RecycleBinEntryResource;

import jakarta.annotation.Generated;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.ws.rs.core.UriInfo;

import java.util.Map;
import java.util.function.BiFunction;

import org.osgi.service.component.ComponentServiceObjects;

/**
 * @author Manuele Castro
 * @generated
 */
@Generated("")
public class Query {

	public static void setRecycleBinEntryResourceComponentServiceObjects(
		ComponentServiceObjects<RecycleBinEntryResource>
			recycleBinEntryResourceComponentServiceObjects) {

		_recycleBinEntryResourceComponentServiceObjects =
			recycleBinEntryResourceComponentServiceObjects;
	}

	/**
	 * Invoke this method with the command line:
	 *
	 * curl -H 'Content-Type: text/plain; charset=utf-8' -X 'POST' 'http://localhost:8080/o/graphql' -d $'{"query": "query {recycleBinEntries(filter: ___, page: ___, pageSize: ___, search: ___, sorts: ___){items {__}, page, pageSize, totalCount}}"}' -u 'test@liferay.com:test'
	 */
	@GraphQLField
	public RecycleBinEntryPage recycleBinEntries(
			@GraphQLName("search") String search,
			@GraphQLName("filter") String filterString,
			@GraphQLName("pageSize") int pageSize,
			@GraphQLName("page") int page,
			@GraphQLName("sort") String sortsString)
		throws Exception {

		return _applyComponentServiceObjects(
			_recycleBinEntryResourceComponentServiceObjects,
			this::_populateResourceContext,
			recycleBinEntryResource -> new RecycleBinEntryPage(
				recycleBinEntryResource.getRecycleBinEntriesPage(
					search,
					_filterBiFunction.apply(
						recycleBinEntryResource, filterString),
					Pagination.of(page, pageSize),
					_sortsBiFunction.apply(
						recycleBinEntryResource, sortsString))));
	}

	/**
	 * Invoke this method with the command line:
	 *
	 * curl -H 'Content-Type: text/plain; charset=utf-8' -X 'POST' 'http://localhost:8080/o/graphql' -d $'{"query": "query {recycleBinEntryByExternalReferenceCode(externalReferenceCode: ___){creator, dateCreated, externalReferenceCode, spaceTitle, title, type}}"}' -u 'test@liferay.com:test'
	 */
	@GraphQLField
	public RecycleBinEntry recycleBinEntryByExternalReferenceCode(
			@GraphQLName("externalReferenceCode") String externalReferenceCode)
		throws Exception {

		return _applyComponentServiceObjects(
			_recycleBinEntryResourceComponentServiceObjects,
			this::_populateResourceContext,
			recycleBinEntryResource ->
				recycleBinEntryResource.
					getRecycleBinEntryByExternalReferenceCode(
						externalReferenceCode));
	}

	@GraphQLName("RecycleBinEntryPage")
	public class RecycleBinEntryPage {

		public RecycleBinEntryPage(Page recycleBinEntryPage) {
			actions = recycleBinEntryPage.getActions();

			items = recycleBinEntryPage.getItems();
			lastPage = recycleBinEntryPage.getLastPage();
			page = recycleBinEntryPage.getPage();
			pageSize = recycleBinEntryPage.getPageSize();
			totalCount = recycleBinEntryPage.getTotalCount();
		}

		@GraphQLField
		protected Map<String, Map<String, String>> actions;

		@GraphQLField
		protected java.util.Collection<RecycleBinEntry> items;

		@GraphQLField
		protected long lastPage;

		@GraphQLField
		protected long page;

		@GraphQLField
		protected long pageSize;

		@GraphQLField
		protected long totalCount;

	}

	private <T, R, E1 extends Throwable, E2 extends Throwable> R
			_applyComponentServiceObjects(
				ComponentServiceObjects<T> componentServiceObjects,
				UnsafeConsumer<T, E1> unsafeConsumer,
				UnsafeFunction<T, R, E2> unsafeFunction)
		throws E1, E2 {

		T resource = componentServiceObjects.getService();

		try {
			unsafeConsumer.accept(resource);

			return unsafeFunction.apply(resource);
		}
		finally {
			componentServiceObjects.ungetService(resource);
		}
	}

	private void _populateResourceContext(
			RecycleBinEntryResource recycleBinEntryResource)
		throws Exception {

		recycleBinEntryResource.setContextAcceptLanguage(_acceptLanguage);
		recycleBinEntryResource.setContextCompany(_company);
		recycleBinEntryResource.setContextHttpServletRequest(
			_httpServletRequest);
		recycleBinEntryResource.setContextHttpServletResponse(
			_httpServletResponse);
		recycleBinEntryResource.setContextUriInfo(_uriInfo);
		recycleBinEntryResource.setContextUser(_user);
		recycleBinEntryResource.setGroupLocalService(_groupLocalService);
		recycleBinEntryResource.setRoleLocalService(_roleLocalService);
	}

	private static ComponentServiceObjects<RecycleBinEntryResource>
		_recycleBinEntryResourceComponentServiceObjects;

	private AcceptLanguage _acceptLanguage;
	private com.liferay.portal.kernel.model.Company _company;
	private BiFunction
		<Object, String, com.liferay.portal.kernel.search.filter.Filter>
			_filterBiFunction;
	private GroupLocalService _groupLocalService;
	private HttpServletRequest _httpServletRequest;
	private HttpServletResponse _httpServletResponse;
	private RoleLocalService _roleLocalService;
	private BiFunction<Object, String, com.liferay.portal.kernel.search.Sort[]>
		_sortsBiFunction;
	private UriInfo _uriInfo;
	private com.liferay.portal.kernel.model.User _user;

}