/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.resource.v1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLTrashLocalServiceUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.test.constants.TestDataConstants;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.trash.model.TrashEntry;
import com.liferay.trash.rest.client.dto.v1_0.Creator;
import com.liferay.trash.rest.client.dto.v1_0.RecycleBinEntry;
import com.liferay.trash.service.TrashEntryLocalServiceUtil;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Manuele Castro
 */
@RunWith(Arquillian.class)
public class RecycleBinEntryResourceTest
	extends BaseRecycleBinEntryResourceTestCase {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

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
			testGetRecycleBinEntryByExternalReferenceCode_addRecycleBinEntry()
		throws Exception {

		User user = UserTestUtil.getAdminUser(testGroup.getCompanyId());

		FileEntry fileEntry = DLAppLocalServiceUtil.addFileEntry(
			null, user.getUserId(), testGroup.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			RandomTestUtil.randomString(), ContentTypes.TEXT_PLAIN,
			TestDataConstants.TEST_BYTE_ARRAY, null, null, null,
			ServiceContextTestUtil.getServiceContext(
				testGroup.getGroupId(), user.getUserId()));

		DLTrashLocalServiceUtil.moveFileEntryToTrash(
			user.getUserId(), fileEntry.getRepositoryId(),
			fileEntry.getFileEntryId());

		TrashEntry trashEntry = TrashEntryLocalServiceUtil.getEntry(
			DLFileEntry.class.getName(), fileEntry.getFileEntryId());

		return new RecycleBinEntry() {
			{
				setCreator(
					() -> new Creator() {
						{
							setId(user.getUserId());
							setName(user.getFullName());
						}
					});
				setDateCreated(trashEntry::getCreateDate);
				setExternalReferenceCode(trashEntry::getExternalReferenceCode);
				setSpaceTitle(testGroup::getGroupKey);
				setTitle(fileEntry.getTitle());
				setType(trashEntry::getClassName);
			}
		};
	}
}
