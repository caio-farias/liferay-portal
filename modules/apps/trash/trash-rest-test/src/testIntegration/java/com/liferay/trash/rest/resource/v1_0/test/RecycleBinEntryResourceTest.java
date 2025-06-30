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
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.constants.TestDataConstants;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.trash.model.TrashEntry;
import com.liferay.trash.rest.client.dto.v1_0.Creator;
import com.liferay.trash.rest.client.dto.v1_0.RecycleBinEntry;
import com.liferay.trash.rest.client.pagination.Page;
import com.liferay.trash.rest.client.pagination.Pagination;
import com.liferay.trash.service.TrashEntryLocalServiceUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class RecycleBinEntryResourceTest
	extends BaseRecycleBinEntryResourceTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();

		_user = UserTestUtil.getAdminUser(testGroup.getCompanyId());
		_startCountDownLatch = new CountDownLatch(1);
	}

	@Override
	public void testGetRecycleBinEntriesSiteGroupPageWithPagination()
		throws Exception {

		super.testGetRecycleBinEntriesSiteGroupPageWithPagination();

		Pagination pagination = Pagination.of(0, 20);

		RecycleBinEntry recycleBinEntry1 = randomRecycleBinEntry();

		recycleBinEntry1.setTitle("someTitle");

		RecycleBinEntry recycleBinEntry2 = randomRecycleBinEntry();

		recycleBinEntry2.setTitle("someTitle1");

		testGetRecycleBinEntriesSiteGroupPage_addRecycleBinEntry(
			testGroup.getGroupId(), recycleBinEntry1);

		testGetRecycleBinEntriesSiteGroupPage_addRecycleBinEntry(
			testGroup.getGroupId(), recycleBinEntry2);

		String searchKeyword = "some";

		Page<RecycleBinEntry> searchResultByKeywordAndSortedByRemovedDate =
			recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
				testGroup.getGroupId(), null, searchKeyword, pagination,
				"removedDate:desc");

		Assert.assertEquals(
			2, searchResultByKeywordAndSortedByRemovedDate.getTotalCount());

		boolean resultConsistent = Boolean.TRUE;
		RecycleBinEntry previousRecycleBinEntry = null;

		for (RecycleBinEntry recycleBinEntry :
				searchResultByKeywordAndSortedByRemovedDate.getItems()) {

			boolean isKeywordPresent = false;

			if (recycleBinEntry.getCreator(
				).getName(
				).toLowerCase(
				).contains(
					searchKeyword
				) ||
				recycleBinEntry.getTitle(
				).toLowerCase(
				).contains(
					searchKeyword
				)) {

				isKeywordPresent = true;
			}

			if (previousRecycleBinEntry != null) {
				resultConsistent &= previousRecycleBinEntry.getDateCreated(
				).after(
					recycleBinEntry.getDateCreated()
				);
			}

			previousRecycleBinEntry = recycleBinEntry;
			Assert.assertEquals(Boolean.TRUE, isKeywordPresent);
		}

		Assert.assertEquals(Boolean.TRUE, resultConsistent);

		Page<RecycleBinEntry> searchResultByKeywordAndSortedByRemovedDate2 =
			recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
				testGroup.getGroupId(), null, "", pagination,
				"removedDate:asc");

		Assert.assertEquals(
			5, searchResultByKeywordAndSortedByRemovedDate2.getTotalCount());

		previousRecycleBinEntry = null;

		for (RecycleBinEntry recycleBinEntry :
				searchResultByKeywordAndSortedByRemovedDate2.getItems()) {

			if (previousRecycleBinEntry != null) {
				resultConsistent &= previousRecycleBinEntry.getDateCreated(
				).before(
					recycleBinEntry.getDateCreated()
				);
			}

			previousRecycleBinEntry = recycleBinEntry;
		}

		Assert.assertEquals(Boolean.TRUE, resultConsistent);

		String assetClassName = FileEntry.class.getName();
		createBookmarksEntryOnRecycleBin();
		Page<RecycleBinEntry>
			searchResultFilteredByAssetClassNameNameFileEntry =
				recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
					testGroup.getGroupId(), assetClassName, "", pagination,
					"removedDate:asc");

		previousRecycleBinEntry = null;

		for (RecycleBinEntry recycleBinEntry :
				searchResultFilteredByAssetClassNameNameFileEntry.getItems()) {

			if (previousRecycleBinEntry != null) {
				resultConsistent &=
					previousRecycleBinEntry.getDateCreated(
					).before(
						recycleBinEntry.getDateCreated()
					) &&
					previousRecycleBinEntry.getType(
					).equals(
						recycleBinEntry.getType()
					);
			}

			previousRecycleBinEntry = recycleBinEntry;
		}

		Assert.assertEquals(Boolean.TRUE, resultConsistent);

		assetClassName = BookmarksEntry.class.getName();
		createBookmarksEntryOnRecycleBin();
		Page<RecycleBinEntry>
			searchResultFilteredByAssetClassNameNameBookmarksEntry =
				recycleBinEntryResource.getRecycleBinEntriesSiteGroupPage(
					testGroup.getGroupId(), assetClassName, "", pagination, "");

		for (RecycleBinEntry recycleBinEntry :
				searchResultFilteredByAssetClassNameNameBookmarksEntry.
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

		_startCountDownLatch.await(1, TimeUnit.SECONDS);

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

	private void createBookmarksEntryOnRecycleBin() throws PortalException {
		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				testGroup.getGroupId(), _user.getUserId());

		BookmarksEntry bookmarksEntry = BookmarksEntryLocalServiceUtil.addEntry(
			_user.getUserId(), testGroup.getGroupId(), 0L,
			RandomTestUtil.randomString(), "http://www.liferay.com",
			RandomTestUtil.randomString(), serviceContext);

		BookmarksEntryLocalServiceUtil.moveEntryToTrash(
			_user.getUserId(), bookmarksEntry);

		TrashEntryLocalServiceUtil.fetchEntry(
			BookmarksEntry.class.getName(), bookmarksEntry.getEntryId());
	}

	private CountDownLatch _startCountDownLatch;
	private User _user;

}