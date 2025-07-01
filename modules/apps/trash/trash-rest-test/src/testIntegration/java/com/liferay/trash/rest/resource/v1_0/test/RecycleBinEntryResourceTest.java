/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.resource.v1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.bookmarks.model.BookmarksEntry;
import com.liferay.bookmarks.service.BookmarksEntryLocalServiceUtil;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLTrashLocalServiceUtil;
import com.liferay.petra.function.UnsafeTriConsumer;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.constants.TestDataConstants;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.trash.model.TrashEntry;
import com.liferay.trash.rest.client.dto.v1_0.Creator;
import com.liferay.trash.rest.client.dto.v1_0.RecycleBinEntry;
import com.liferay.trash.rest.client.pagination.Page;
import com.liferay.trash.rest.client.pagination.Pagination;
import com.liferay.trash.service.TrashEntryLocalServiceUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuele Castro
 * @author Caio Farias
 */
@RunWith(Arquillian.class)
public class RecycleBinEntryResourceTest
	extends BaseRecycleBinEntryResourceTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		_user = UserTestUtil.getAdminUser(testGroup.getCompanyId());
	}

	@Override
	@Test
	public void testGetRecycleBinEntriesSiteGroupPageWithPagination()
		throws Exception {

		super.testGetRecycleBinEntriesSiteGroupPageWithPagination();

		Pagination pagination = Pagination.of(0, 20);

		String searchKeyword = RandomTestUtil.randomString();

		RecycleBinEntry recycleBinEntry1 = randomRecycleBinEntry();

		recycleBinEntry1.setTitle(searchKeyword + recycleBinEntry1.getTitle());

		RecycleBinEntry recycleBinEntry2 = randomRecycleBinEntry();

		recycleBinEntry2.setTitle(searchKeyword + recycleBinEntry2.getTitle());

		testGetRecycleBinEntriesSiteGroupPage_addRecycleBinEntry(
			testGroup.getGroupId(), recycleBinEntry1);

		testGetRecycleBinEntriesSiteGroupPage_addRecycleBinEntry(
			testGroup.getGroupId(), recycleBinEntry2);

		Page<RecycleBinEntry> searchResultByKeywordAndSortedByRemovedDatePage1 =
			recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
				testGroup.getGroupId(), null, searchKeyword, pagination,
				Field.REMOVED_DATE + ":desc");

		Assert.assertEquals(
			2,
			searchResultByKeywordAndSortedByRemovedDatePage1.getTotalCount());

		for (RecycleBinEntry recycleBinEntry :
				searchResultByKeywordAndSortedByRemovedDatePage1.getItems()) {

			Assert.assertEquals(
				Boolean.TRUE,
				StringUtil.startsWith(
					recycleBinEntry.getCreator(
					).getName(),
					searchKeyword) ||
				StringUtil.startsWith(
					recycleBinEntry.getTitle(), searchKeyword));
		}

		Page<RecycleBinEntry> searchResultByKeywordAndSortedByRemovedDatePage2 =
			recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
				testGroup.getGroupId(), null, "", pagination,
				Field.REMOVED_DATE + ":asc");

		Assert.assertEquals(
			5,
			searchResultByKeywordAndSortedByRemovedDatePage2.getTotalCount());

		String assetClassName = DLFileEntry.class.getName();

		_createBookmarksEntryOnRecycleBin();

		Page<RecycleBinEntry>
			searchResultFilteredByAssetClassNameNameFileEntryPage =
				recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
					testGroup.getGroupId(), assetClassName, "", pagination,
					Field.REMOVED_DATE + ":asc");

		for (RecycleBinEntry recycleBinEntry :
				searchResultFilteredByAssetClassNameNameFileEntryPage.
					getItems()) {

			Assert.assertEquals(assetClassName, recycleBinEntry.getType());
		}

		assetClassName = BookmarksEntry.class.getName();

		_createBookmarksEntryOnRecycleBin();

		Page<RecycleBinEntry>
			searchResultFilteredByAssetClassNameNameBookmarksEntryPage =
				recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
					testGroup.getGroupId(), assetClassName, "", pagination, "");

		for (RecycleBinEntry recycleBinEntry :
				searchResultFilteredByAssetClassNameNameBookmarksEntryPage.
					getItems()) {

			Assert.assertEquals(assetClassName, recycleBinEntry.getType());
		}
	}

	@Override
	@Test
	public void testGetRecycleBinEntryByExternalReferenceCode()
		throws Exception {

		super.testGetRecycleBinEntryByExternalReferenceCode();

		assertHttpResponseStatusCode(
			404,
			recycleBinEntryResource.
				getRecycleBinEntryByExternalReferenceCodeHttpResponse(
					"externalReferenceCode"));
	}

	@Override
	protected RecycleBinEntry
			testGetRecycleBinEntriesSiteGroupPage_addRecycleBinEntry(
				Long siteGroupId, RecycleBinEntry recycleBinEntry)
		throws Exception {

		FileEntry fileEntry = DLAppLocalServiceUtil.addFileEntry(
			null, _user.getUserId(), siteGroupId,
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			recycleBinEntry.getTitle(), ContentTypes.TEXT_PLAIN,
			TestDataConstants.TEST_BYTE_ARRAY, null, null, null,
			ServiceContextTestUtil.getServiceContext(
				siteGroupId, _user.getUserId()));

		DLTrashLocalServiceUtil.moveFileEntryToTrash(
			_user.getUserId(), fileEntry.getRepositoryId(),
			fileEntry.getFileEntryId());

		return new RecycleBinEntry() {
			{
				setCreator(
					() -> new Creator() {
						{
							setId(_user.getUserId());
							setName(_user.getFullName());
						}
					});
				setExternalReferenceCode(
					recycleBinEntry::getExternalReferenceCode);
				setSpaceTitle(testGroup::getGroupKey);
				setTitle(fileEntry.getTitle());
				setType(recycleBinEntry::getType);
			}
		};
	}

	@Override
	protected Long testGetRecycleBinEntriesSiteGroupPage_getSiteGroupId()
		throws Exception {

		return testGroup.getGroupId();
	}

	@Override
	protected void testGetRecycleBinEntriesSiteGroupPageWithSort(
			EntityField.Type type,
			UnsafeTriConsumer
				<EntityField, RecycleBinEntry, RecycleBinEntry, Exception>
					unsafeTriConsumer)
		throws Exception {

		if (type == EntityField.Type.STRING) {
			return;
		}

		super.testGetRecycleBinEntriesSiteGroupPageWithSort(
			type, unsafeTriConsumer);
	}

	@Override
	protected RecycleBinEntry
			testGetRecycleBinEntryByExternalReferenceCode_addRecycleBinEntry()
		throws Exception {

		FileEntry fileEntry = DLAppLocalServiceUtil.addFileEntry(
			null, _user.getUserId(), testGroup.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			RandomTestUtil.randomString(), ContentTypes.TEXT_PLAIN,
			TestDataConstants.TEST_BYTE_ARRAY, null, null, null,
			ServiceContextTestUtil.getServiceContext(
				testGroup.getGroupId(), _user.getUserId()));

		DLTrashLocalServiceUtil.moveFileEntryToTrash(
			_user.getUserId(), fileEntry.getRepositoryId(),
			fileEntry.getFileEntryId());

		TrashEntry trashEntry = TrashEntryLocalServiceUtil.getEntry(
			DLFileEntry.class.getName(), fileEntry.getFileEntryId());

		return new RecycleBinEntry() {
			{
				setExternalReferenceCode(trashEntry::getExternalReferenceCode);
			}
		};
	}

	private void _createBookmarksEntryOnRecycleBin() {
		try {
			ServiceContext serviceContext =
				serviceContext = ServiceContextTestUtil.getServiceContext(
					testGroup.getGroupId(), _user.getUserId());

			BookmarksEntry bookmarksEntry =
				BookmarksEntryLocalServiceUtil.addEntry(
					_user.getUserId(), testGroup.getGroupId(), 0L,
					RandomTestUtil.randomString(), "http://www.liferay.com",
					RandomTestUtil.randomString(), serviceContext);

			BookmarksEntryLocalServiceUtil.moveEntryToTrash(
				_user.getUserId(), bookmarksEntry);

			TrashEntryLocalServiceUtil.fetchEntry(
				BookmarksEntry.class.getName(), bookmarksEntry.getEntryId());
		}
		catch (PortalException portalException) {
			throw new RuntimeException(portalException);
		}
	}

	private User _user;

}