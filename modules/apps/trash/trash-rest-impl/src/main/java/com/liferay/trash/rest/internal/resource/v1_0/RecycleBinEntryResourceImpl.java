/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.internal.resource.v1_0;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetEntryService;
import com.liferay.depot.model.DepotEntry;
import com.liferay.headless.delivery.dto.v1_0.util.CreatorUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.GroupService;
import com.liferay.portal.kernel.service.UserService;
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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Manuele Castro
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/recycle-bin-entry.properties",
	scope = ServiceScope.PROTOTYPE, service = RecycleBinEntryResource.class
)
public class RecycleBinEntryResourceImpl
	extends BaseRecycleBinEntryResourceImpl {

	@Override
	public Page<RecycleBinEntry> getRecycleBinEntriesSiteGroupPage(
			Long siteGroupId, Pagination pagination)
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

		return SearchUtil.search(
			null,
			booleanQuery -> {
			},
			null, TrashEntry.class.getName(), null, pagination,
			queryConfig -> queryConfig.setSelectedFieldNames(
				Field.ENTRY_CLASS_PK),
			searchContext -> {
				searchContext.setCompanyId(contextCompany.getCompanyId());
				searchContext.setGroupIds(ArrayUtil.toLongArray(groupIds));
			},
			null,
			document -> _toRecycleBinEntry(
				_trashEntryLocalService.fetchEntry(
					GetterUtil.getString(document.get(Field.ENTRY_CLASS_NAME)),
					GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK)))));
	}

	@Override
	public RecycleBinEntry getRecycleBinEntryByExternalReferenceCode(
			String externalReferenceCode)
		throws Exception {

		return _toRecycleBinEntry(
			_trashEntryLocalService.getTrashEntryByExternalReferenceCode(
				externalReferenceCode, contextCompany.getCompanyId()));
	}

	private RecycleBinEntry _toRecycleBinEntry(TrashEntry trashEntry)
		throws PortalException {

		AssetEntry assetEntry = _assetEntryService.getEntry(
			trashEntry.getClassName(), trashEntry.getClassPK());

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
				setTitle(assetEntry::getTitle);
				setType(trashEntry::getClassName);
			}
		};
	}

	@Reference
	private AssetEntryService _assetEntryService;

	@Reference
	private GroupService _groupService;

	@Reference
	private Portal _portal;

	@Reference
	private TrashEntryLocalService _trashEntryLocalService;

	@Reference
	private UserService _userService;

}