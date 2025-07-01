/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.internal.resource.v1_0;

import com.liferay.depot.model.DepotEntry;
import com.liferay.headless.delivery.dto.v1_0.util.CreatorUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.ExistsFilter;
import com.liferay.portal.kernel.search.generic.MultiMatchQuery;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.GroupService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.trash.TrashHandler;
import com.liferay.portal.kernel.trash.TrashHandlerRegistryUtil;
import com.liferay.portal.kernel.trash.TrashRenderer;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LinkedHashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.util.SearchUtil;
import com.liferay.trash.model.TrashEntry;
import com.liferay.trash.rest.dto.v1_0.RecycleBinEntry;
import com.liferay.trash.rest.resource.v1_0.RecycleBinEntryResource;
import com.liferay.trash.service.TrashEntryLocalService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Manuele Castro
 * @author Caio Farias
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/recycle-bin-entry.properties",
	scope = ServiceScope.PROTOTYPE, service = RecycleBinEntryResource.class
)
public class RecycleBinEntryResourceImpl
	extends BaseRecycleBinEntryResourceImpl {

	@Override
	public Page<RecycleBinEntry> getRecycleBinEntriesSiteGroupPage(
			Long siteGroupId, String assetClassName, String search,
			Pagination pagination, Sort[] sorts)
		throws Exception {

		Group group = _groupService.getGroup(siteGroupId);
		List<Long> groupIds = new ArrayList<>();

		if (group.isCMS()) {
			groupIds.addAll(
				ListUtil.toList(
					_groupService.search(
						contextCompany.getCompanyId(),
						new long[] {
							_portal.getClassNameId(DepotEntry.class.getName())
						},
						StringPool.BLANK,
						LinkedHashMapBuilder.<String, Object>put(
							"actionId", ActionKeys.VIEW
						).put(
							"site", Boolean.FALSE
						).build(),
						pagination.getStartPosition(),
						pagination.getEndPosition(), null),
					Group::getGroupId));
		}
		else {
			groupIds.add(siteGroupId);
		}

		if (sorts != null) {
			for (Sort sort : sorts) {
				if (Objects.equals(sort.getFieldName(), Field.REMOVED_DATE)) {
					sort.setType(Sort.LONG_TYPE);

					continue;
				}

				sort.setType(Sort.SCORE_TYPE);
			}
		}

		return SearchUtil.search(
			null,
			booleanQuery -> {
				BooleanFilter preBooleanFilter =
					booleanQuery.getPreBooleanFilter();

				preBooleanFilter.add(new ExistsFilter(Field.TYPE), BooleanClauseOccur.MUST);

				if (assetClassName != null) {
					MultiMatchQuery multiMatchQuery = new MultiMatchQuery(
						assetClassName);

					multiMatchQuery.addFields(
						Field.ENTRY_CLASS_NAME, Field.ROOT_ENTRY_CLASS_NAME);

					booleanQuery.add(multiMatchQuery, BooleanClauseOccur.MUST);
				}
			},
			null, TrashEntry.class.getName(), search, pagination,
			queryConfig -> queryConfig.setSelectedFieldNames(
				Field.ENTRY_CLASS_PK, Field.ENTRY_CLASS_NAME,
				Field.ROOT_ENTRY_CLASS_PK, Field.ROOT_ENTRY_CLASS_NAME),
			searchContext -> {
				QueryConfig queryConfig = searchContext.getQueryConfig();

				queryConfig.setHighlightEnabled(false);
				queryConfig.setScoreEnabled(false);

				searchContext.setCompanyId(contextCompany.getCompanyId());
				searchContext.setGroupIds(ArrayUtil.toLongArray(groupIds));
			},
			sorts,
			document -> {
				TrashEntry trashEntry = _trashEntryLocalService.fetchEntry(
					GetterUtil.getString(document.get(Field.ENTRY_CLASS_NAME)),
					GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK)));

				if (trashEntry == null) {
					trashEntry = _trashEntryLocalService.fetchEntry(
						GetterUtil.getString(
							document.get(Field.ROOT_ENTRY_CLASS_NAME)),
						GetterUtil.getLong(
							document.get(Field.ROOT_ENTRY_CLASS_PK)));
				}

				return _toRecycleBinEntry(trashEntry);
			});
	}

	@Override
	public RecycleBinEntry getRecycleBinEntryByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		return _toRecycleBinEntry(
			_trashEntryLocalService.getTrashEntryByExternalReferenceCode(
				externalReferenceCode, contextCompany.getCompanyId()));
	}

	private ServiceContext _getServiceContext(long groupId) {
		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(contextCompany.getCompanyId());
		serviceContext.setRequest(contextHttpServletRequest);
		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(contextUser.getUserId());

		return serviceContext;
	}

	private RecycleBinEntry _toRecycleBinEntry(TrashEntry trashEntry)
		throws PortalException {

		Group group = _groupService.getGroup(trashEntry.getGroupId());

		return new RecycleBinEntry() {
			{
				setCreator(
					() -> CreatorUtil.toCreator(
						new DefaultDTOConverterContext(
							null, null, null, contextUriInfo, null),
						_portal,
						_userService.getUserById(trashEntry.getUserId())));
				setDateCreated(trashEntry::getCreateDate);
				setExternalReferenceCode(trashEntry::getExternalReferenceCode);
				setSpaceTitle(group::getGroupKey);
				setTitle(
					() -> {
						ServiceContextThreadLocal.pushServiceContext(
							_getServiceContext(trashEntry.getGroupId()));

						try {
							TrashHandler trashHandler =
								TrashHandlerRegistryUtil.getTrashHandler(
									trashEntry.getClassName());

							TrashRenderer trashRenderer =
								trashHandler.getTrashRenderer(
									trashEntry.getClassPK());

							return trashRenderer.getTitle(
								contextAcceptLanguage.getPreferredLocale());
						}
						finally {
							ServiceContextThreadLocal.popServiceContext();
						}
					});
				setType(trashEntry::getClassName);
			}
		};
	}

	@Reference
	private GroupService _groupService;

	@Reference
	private Portal _portal;

	@Reference
	private TrashEntryLocalService _trashEntryLocalService;

	@Reference
	private UserService _userService;

}