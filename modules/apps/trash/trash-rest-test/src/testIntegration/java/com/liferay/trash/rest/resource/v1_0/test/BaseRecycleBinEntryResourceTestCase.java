/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.resource.v1_0.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import com.liferay.petra.function.UnsafeTriConsumer;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.search.test.rule.SearchTestRule;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.vulcan.resource.EntityModelResource;
import com.liferay.trash.rest.client.dto.v1_0.RecycleBinEntry;
import com.liferay.trash.rest.client.http.HttpInvoker;
import com.liferay.trash.rest.client.pagination.Page;
import com.liferay.trash.rest.client.pagination.Pagination;
import com.liferay.trash.rest.client.resource.v1_0.RecycleBinEntryResource;
import com.liferay.trash.rest.client.serdes.v1_0.RecycleBinEntrySerDes;

import jakarta.annotation.Generated;

import jakarta.ws.rs.core.MultivaluedHashMap;

import java.lang.reflect.Method;

import java.text.Format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 * @generated
 */
@Generated("")
public abstract class BaseRecycleBinEntryResourceTestCase {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_format = FastDateFormatFactoryUtil.getSimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	@Before
	public void setUp() throws Exception {
		irrelevantGroup = GroupTestUtil.addGroup();
		testGroup = GroupTestUtil.addGroup();

		testCompany = CompanyLocalServiceUtil.getCompany(
			testGroup.getCompanyId());

		_recycleBinEntryResource.setContextCompany(testCompany);

		_testCompanyAdminUser = UserTestUtil.getAdminUser(
			testCompany.getCompanyId());

		recycleBinEntryResource = RecycleBinEntryResource.builder(
		).authentication(
			_testCompanyAdminUser.getEmailAddress(),
			PropsValues.DEFAULT_ADMIN_PASSWORD
		).endpoint(
			testCompany.getVirtualHostname(), 8080, "http"
		).locale(
			LocaleUtil.getDefault()
		).build();
	}

	@After
	public void tearDown() throws Exception {
		GroupTestUtil.deleteGroup(irrelevantGroup);
		GroupTestUtil.deleteGroup(testGroup);
	}

	@Test
	public void testClientSerDesToDTO() throws Exception {
		ObjectMapper objectMapper = getClientSerDesObjectMapper();

		RecycleBinEntry recycleBinEntry1 = randomRecycleBinEntry();

		String json = objectMapper.writeValueAsString(recycleBinEntry1);

		RecycleBinEntry recycleBinEntry2 = RecycleBinEntrySerDes.toDTO(json);

		Assert.assertTrue(equals(recycleBinEntry1, recycleBinEntry2));
	}

	@Test
	public void testClientSerDesToJSON() throws Exception {
		ObjectMapper objectMapper = getClientSerDesObjectMapper();

		RecycleBinEntry recycleBinEntry = randomRecycleBinEntry();

		String json1 = objectMapper.writeValueAsString(recycleBinEntry);
		String json2 = RecycleBinEntrySerDes.toJSON(recycleBinEntry);

		Assert.assertEquals(
			objectMapper.readTree(json1), objectMapper.readTree(json2));
	}

	protected ObjectMapper getClientSerDesObjectMapper() {
		return new ObjectMapper() {
			{
				configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
				configure(
					SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
				enable(SerializationFeature.INDENT_OUTPUT);
				setDateFormat(new ISO8601DateFormat());
				setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
				setSerializationInclusion(JsonInclude.Include.NON_NULL);
				setVisibility(
					PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
				setVisibility(
					PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
			}
		};
	}

	@Test
	public void testEscapeRegexInStringFields() throws Exception {
		String regex = "^[0-9]+(\\.[0-9]{1,2})\"?";

		RecycleBinEntry recycleBinEntry = randomRecycleBinEntry();

		recycleBinEntry.setExternalReferenceCode(regex);
		recycleBinEntry.setSpaceTitle(regex);
		recycleBinEntry.setTitle(regex);

		String json = RecycleBinEntrySerDes.toJSON(recycleBinEntry);

		Assert.assertFalse(json.contains(regex));

		recycleBinEntry = RecycleBinEntrySerDes.toDTO(json);

		Assert.assertEquals(regex, recycleBinEntry.getExternalReferenceCode());
		Assert.assertEquals(regex, recycleBinEntry.getSpaceTitle());
		Assert.assertEquals(regex, recycleBinEntry.getTitle());
	}

	@Test
	public void testGetRecycleBinEntriesPage() throws Exception {
		Page<RecycleBinEntry> page =
			recycleBinEntryResource.getRecycleBinEntriesPage(
				null, null, Pagination.of(1, 10), null);

		long totalCount = page.getTotalCount();

		RecycleBinEntry recycleBinEntry1 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		RecycleBinEntry recycleBinEntry2 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		page = recycleBinEntryResource.getRecycleBinEntriesPage(
			null, null, Pagination.of(1, 10), null);

		Assert.assertEquals(totalCount + 2, page.getTotalCount());

		assertContains(
			recycleBinEntry1, (List<RecycleBinEntry>)page.getItems());
		assertContains(
			recycleBinEntry2, (List<RecycleBinEntry>)page.getItems());
		assertValid(page, testGetRecycleBinEntriesPage_getExpectedActions());
	}

	protected Map<String, Map<String, String>>
			testGetRecycleBinEntriesPage_getExpectedActions()
		throws Exception {

		Map<String, Map<String, String>> expectedActions = new HashMap<>();

		return expectedActions;
	}

	@Test
	public void testGetRecycleBinEntriesPageWithFilterDateTimeEquals()
		throws Exception {

		List<EntityField> entityFields = getEntityFields(
			EntityField.Type.DATE_TIME);

		if (entityFields.isEmpty()) {
			return;
		}

		RecycleBinEntry recycleBinEntry1 = randomRecycleBinEntry();

		recycleBinEntry1 = testGetRecycleBinEntriesPage_addRecycleBinEntry(
			recycleBinEntry1);

		for (EntityField entityField : entityFields) {
			Page<RecycleBinEntry> page =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null,
					getFilterString(entityField, "between", recycleBinEntry1),
					Pagination.of(1, 2), null);

			assertEquals(
				Collections.singletonList(recycleBinEntry1),
				(List<RecycleBinEntry>)page.getItems());
		}
	}

	@Test
	public void testGetRecycleBinEntriesPageWithFilterDoubleEquals()
		throws Exception {

		testGetRecycleBinEntriesPageWithFilter("eq", EntityField.Type.DOUBLE);
	}

	@Test
	public void testGetRecycleBinEntriesPageWithFilterStringContains()
		throws Exception {

		testGetRecycleBinEntriesPageWithFilter(
			"contains", EntityField.Type.STRING);
	}

	@Test
	public void testGetRecycleBinEntriesPageWithFilterStringEquals()
		throws Exception {

		testGetRecycleBinEntriesPageWithFilter("eq", EntityField.Type.STRING);
	}

	@Test
	public void testGetRecycleBinEntriesPageWithFilterStringStartsWith()
		throws Exception {

		testGetRecycleBinEntriesPageWithFilter(
			"startswith", EntityField.Type.STRING);
	}

	protected void testGetRecycleBinEntriesPageWithFilter(
			String operator, EntityField.Type type)
		throws Exception {

		List<EntityField> entityFields = getEntityFields(type);

		if (entityFields.isEmpty()) {
			return;
		}

		RecycleBinEntry recycleBinEntry1 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		@SuppressWarnings("PMD.UnusedLocalVariable")
		RecycleBinEntry recycleBinEntry2 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		for (EntityField entityField : entityFields) {
			Page<RecycleBinEntry> page =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null,
					getFilterString(entityField, operator, recycleBinEntry1),
					Pagination.of(1, 2), null);

			assertEquals(
				Collections.singletonList(recycleBinEntry1),
				(List<RecycleBinEntry>)page.getItems());
		}
	}

	@Test
	public void testGetRecycleBinEntriesPageWithPagination() throws Exception {
		Page<RecycleBinEntry> recycleBinEntriesPage =
			recycleBinEntryResource.getRecycleBinEntriesPage(
				null, null, null, null);

		int totalCount = GetterUtil.getInteger(
			recycleBinEntriesPage.getTotalCount());

		RecycleBinEntry recycleBinEntry1 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		RecycleBinEntry recycleBinEntry2 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		RecycleBinEntry recycleBinEntry3 =
			testGetRecycleBinEntriesPage_addRecycleBinEntry(
				randomRecycleBinEntry());

		// See com.liferay.portal.vulcan.internal.configuration.HeadlessAPICompanyConfiguration#pageSizeLimit

		int pageSizeLimit = 500;

		if (totalCount >= (pageSizeLimit - 2)) {
			Page<RecycleBinEntry> page1 =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null,
					Pagination.of(
						(int)Math.ceil((totalCount + 1.0) / pageSizeLimit),
						pageSizeLimit),
					null);

			Assert.assertEquals(totalCount + 3, page1.getTotalCount());

			assertContains(
				recycleBinEntry1, (List<RecycleBinEntry>)page1.getItems());

			Page<RecycleBinEntry> page2 =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null,
					Pagination.of(
						(int)Math.ceil((totalCount + 2.0) / pageSizeLimit),
						pageSizeLimit),
					null);

			assertContains(
				recycleBinEntry2, (List<RecycleBinEntry>)page2.getItems());

			Page<RecycleBinEntry> page3 =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null,
					Pagination.of(
						(int)Math.ceil((totalCount + 3.0) / pageSizeLimit),
						pageSizeLimit),
					null);

			assertContains(
				recycleBinEntry3, (List<RecycleBinEntry>)page3.getItems());
		}
		else {
			Page<RecycleBinEntry> page1 =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null, Pagination.of(1, totalCount + 2), null);

			List<RecycleBinEntry> recycleBinEntries1 =
				(List<RecycleBinEntry>)page1.getItems();

			Assert.assertEquals(
				recycleBinEntries1.toString(), totalCount + 2,
				recycleBinEntries1.size());

			Page<RecycleBinEntry> page2 =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null, Pagination.of(2, totalCount + 2), null);

			Assert.assertEquals(totalCount + 3, page2.getTotalCount());

			List<RecycleBinEntry> recycleBinEntries2 =
				(List<RecycleBinEntry>)page2.getItems();

			Assert.assertEquals(
				recycleBinEntries2.toString(), 1, recycleBinEntries2.size());

			Page<RecycleBinEntry> page3 =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null, Pagination.of(1, (int)totalCount + 3), null);

			assertContains(
				recycleBinEntry1, (List<RecycleBinEntry>)page3.getItems());
			assertContains(
				recycleBinEntry2, (List<RecycleBinEntry>)page3.getItems());
			assertContains(
				recycleBinEntry3, (List<RecycleBinEntry>)page3.getItems());
		}
	}

	@Test
	public void testGetRecycleBinEntriesPageWithSortDateTime()
		throws Exception {

		testGetRecycleBinEntriesPageWithSort(
			EntityField.Type.DATE_TIME,
			(entityField, recycleBinEntry1, recycleBinEntry2) -> {
				BeanTestUtil.setProperty(
					recycleBinEntry1, entityField.getName(),
					new Date(System.currentTimeMillis() - (2 * Time.MINUTE)));
			});
	}

	@Test
	public void testGetRecycleBinEntriesPageWithSortDouble() throws Exception {
		testGetRecycleBinEntriesPageWithSort(
			EntityField.Type.DOUBLE,
			(entityField, recycleBinEntry1, recycleBinEntry2) -> {
				BeanTestUtil.setProperty(
					recycleBinEntry1, entityField.getName(), 0.1);
				BeanTestUtil.setProperty(
					recycleBinEntry2, entityField.getName(), 0.5);
			});
	}

	@Test
	public void testGetRecycleBinEntriesPageWithSortInteger() throws Exception {
		testGetRecycleBinEntriesPageWithSort(
			EntityField.Type.INTEGER,
			(entityField, recycleBinEntry1, recycleBinEntry2) -> {
				BeanTestUtil.setProperty(
					recycleBinEntry1, entityField.getName(), 0);
				BeanTestUtil.setProperty(
					recycleBinEntry2, entityField.getName(), 1);
			});
	}

	@Test
	public void testGetRecycleBinEntriesPageWithSortString() throws Exception {
		testGetRecycleBinEntriesPageWithSort(
			EntityField.Type.STRING,
			(entityField, recycleBinEntry1, recycleBinEntry2) -> {
				Class<?> clazz = recycleBinEntry1.getClass();

				String entityFieldName = entityField.getName();

				Method method = clazz.getMethod(
					"get" + StringUtil.upperCaseFirstLetter(entityFieldName));

				Class<?> returnType = method.getReturnType();

				if (returnType.isAssignableFrom(Map.class)) {
					BeanTestUtil.setProperty(
						recycleBinEntry1, entityFieldName,
						Collections.singletonMap("Aaa", "Aaa"));
					BeanTestUtil.setProperty(
						recycleBinEntry2, entityFieldName,
						Collections.singletonMap("Bbb", "Bbb"));
				}
				else if (entityFieldName.contains("email")) {
					BeanTestUtil.setProperty(
						recycleBinEntry1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
					BeanTestUtil.setProperty(
						recycleBinEntry2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
				}
				else {
					BeanTestUtil.setProperty(
						recycleBinEntry1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
					BeanTestUtil.setProperty(
						recycleBinEntry2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
				}
			});
	}

	protected void testGetRecycleBinEntriesPageWithSort(
			EntityField.Type type,
			UnsafeTriConsumer
				<EntityField, RecycleBinEntry, RecycleBinEntry, Exception>
					unsafeTriConsumer)
		throws Exception {

		List<EntityField> entityFields = getEntityFields(type);

		if (entityFields.isEmpty()) {
			return;
		}

		RecycleBinEntry recycleBinEntry1 = randomRecycleBinEntry();
		RecycleBinEntry recycleBinEntry2 = randomRecycleBinEntry();

		for (EntityField entityField : entityFields) {
			unsafeTriConsumer.accept(
				entityField, recycleBinEntry1, recycleBinEntry2);
		}

		recycleBinEntry1 = testGetRecycleBinEntriesPage_addRecycleBinEntry(
			recycleBinEntry1);

		recycleBinEntry2 = testGetRecycleBinEntriesPage_addRecycleBinEntry(
			recycleBinEntry2);

		Page<RecycleBinEntry> page =
			recycleBinEntryResource.getRecycleBinEntriesPage(
				null, null, null, null);

		for (EntityField entityField : entityFields) {
			Page<RecycleBinEntry> ascPage =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null, Pagination.of(1, (int)page.getTotalCount() + 1),
					entityField.getName() + ":asc");

			assertContains(
				recycleBinEntry1, (List<RecycleBinEntry>)ascPage.getItems());
			assertContains(
				recycleBinEntry2, (List<RecycleBinEntry>)ascPage.getItems());

			Page<RecycleBinEntry> descPage =
				recycleBinEntryResource.getRecycleBinEntriesPage(
					null, null, Pagination.of(1, (int)page.getTotalCount() + 1),
					entityField.getName() + ":desc");

			assertContains(
				recycleBinEntry2, (List<RecycleBinEntry>)descPage.getItems());
			assertContains(
				recycleBinEntry1, (List<RecycleBinEntry>)descPage.getItems());
		}
	}

	protected RecycleBinEntry testGetRecycleBinEntriesPage_addRecycleBinEntry(
			RecycleBinEntry recycleBinEntry)
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testGraphQLGetRecycleBinEntriesPage() throws Exception {
		Assert.assertTrue(false);
	}

	@Test
	public void testGetRecycleBinEntryByExternalReferenceCode()
		throws Exception {

		Assert.assertTrue(false);
	}

	@Test
	public void testGraphQLGetRecycleBinEntryByExternalReferenceCode()
		throws Exception {

		Assert.assertTrue(true);
	}

	@Test
	public void testGraphQLGetRecycleBinEntryByExternalReferenceCodeNotFound()
		throws Exception {

		Assert.assertTrue(true);
	}

	@Rule
	public SearchTestRule searchTestRule = new SearchTestRule();

	protected void assertContains(
		RecycleBinEntry recycleBinEntry,
		List<RecycleBinEntry> recycleBinEntries) {

		boolean contains = false;

		for (RecycleBinEntry item : recycleBinEntries) {
			if (equals(recycleBinEntry, item)) {
				contains = true;

				break;
			}
		}

		Assert.assertTrue(
			recycleBinEntries + " does not contain " + recycleBinEntry,
			contains);
	}

	protected void assertHttpResponseStatusCode(
		int expectedHttpResponseStatusCode,
		HttpInvoker.HttpResponse actualHttpResponse) {

		Assert.assertEquals(
			expectedHttpResponseStatusCode, actualHttpResponse.getStatusCode());
	}

	protected void assertEquals(
		RecycleBinEntry recycleBinEntry1, RecycleBinEntry recycleBinEntry2) {

		Assert.assertTrue(
			recycleBinEntry1 + " does not equal " + recycleBinEntry2,
			equals(recycleBinEntry1, recycleBinEntry2));
	}

	protected void assertEquals(
		List<RecycleBinEntry> recycleBinEntries1,
		List<RecycleBinEntry> recycleBinEntries2) {

		Assert.assertEquals(
			recycleBinEntries1.size(), recycleBinEntries2.size());

		for (int i = 0; i < recycleBinEntries1.size(); i++) {
			RecycleBinEntry recycleBinEntry1 = recycleBinEntries1.get(i);
			RecycleBinEntry recycleBinEntry2 = recycleBinEntries2.get(i);

			assertEquals(recycleBinEntry1, recycleBinEntry2);
		}
	}

	protected void assertEqualsIgnoringOrder(
		List<RecycleBinEntry> recycleBinEntries1,
		List<RecycleBinEntry> recycleBinEntries2) {

		Assert.assertEquals(
			recycleBinEntries1.size(), recycleBinEntries2.size());

		for (RecycleBinEntry recycleBinEntry1 : recycleBinEntries1) {
			boolean contains = false;

			for (RecycleBinEntry recycleBinEntry2 : recycleBinEntries2) {
				if (equals(recycleBinEntry1, recycleBinEntry2)) {
					contains = true;

					break;
				}
			}

			Assert.assertTrue(
				recycleBinEntries2 + " does not contain " + recycleBinEntry1,
				contains);
		}
	}

	protected void assertValid(RecycleBinEntry recycleBinEntry)
		throws Exception {

		boolean valid = true;

		if (recycleBinEntry.getDateCreated() == null) {
			valid = false;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			if (Objects.equals("creator", additionalAssertFieldName)) {
				if (recycleBinEntry.getCreator() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals(
					"externalReferenceCode", additionalAssertFieldName)) {

				if (recycleBinEntry.getExternalReferenceCode() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("spaceTitle", additionalAssertFieldName)) {
				if (recycleBinEntry.getSpaceTitle() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("title", additionalAssertFieldName)) {
				if (recycleBinEntry.getTitle() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("type", additionalAssertFieldName)) {
				if (recycleBinEntry.getType() == null) {
					valid = false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		Assert.assertTrue(valid);
	}

	protected void assertValid(Page<RecycleBinEntry> page) {
		assertValid(page, Collections.emptyMap());
	}

	protected void assertValid(
		Page<RecycleBinEntry> page,
		Map<String, Map<String, String>> expectedActions) {

		boolean valid = false;

		java.util.Collection<RecycleBinEntry> recycleBinEntries =
			page.getItems();

		int size = recycleBinEntries.size();

		if ((page.getLastPage() > 0) && (page.getPage() > 0) &&
			(page.getPageSize() > 0) && (page.getTotalCount() > 0) &&
			(size > 0)) {

			valid = true;
		}

		Assert.assertTrue(valid);

		assertValid(page.getActions(), expectedActions);
	}

	protected void assertValid(
		Map<String, Map<String, String>> actions1,
		Map<String, Map<String, String>> actions2) {

		for (String key : actions2.keySet()) {
			Map action = actions1.get(key);

			Assert.assertNotNull(key + " does not contain an action", action);

			Map<String, String> expectedAction = actions2.get(key);

			Assert.assertEquals(
				expectedAction.get("method"), action.get("method"));
			Assert.assertEquals(expectedAction.get("href"), action.get("href"));
		}
	}

	protected String[] getAdditionalAssertFieldNames() {
		return new String[0];
	}

	protected List<GraphQLField> getGraphQLFields() throws Exception {
		List<GraphQLField> graphQLFields = new ArrayList<>();

		for (java.lang.reflect.Field field :
				getDeclaredFields(
					com.liferay.trash.rest.dto.v1_0.RecycleBinEntry.class)) {

			if (!ArrayUtil.contains(
					getAdditionalAssertFieldNames(), field.getName())) {

				continue;
			}

			graphQLFields.addAll(getGraphQLFields(field));
		}

		return graphQLFields;
	}

	protected List<GraphQLField> getGraphQLFields(
			java.lang.reflect.Field... fields)
		throws Exception {

		List<GraphQLField> graphQLFields = new ArrayList<>();

		for (java.lang.reflect.Field field : fields) {
			com.liferay.portal.vulcan.graphql.annotation.GraphQLField
				vulcanGraphQLField = field.getAnnotation(
					com.liferay.portal.vulcan.graphql.annotation.GraphQLField.
						class);

			if (vulcanGraphQLField != null) {
				Class<?> clazz = field.getType();

				if (clazz.isArray()) {
					clazz = clazz.getComponentType();
				}

				List<GraphQLField> childrenGraphQLFields = getGraphQLFields(
					getDeclaredFields(clazz));

				graphQLFields.add(
					new GraphQLField(field.getName(), childrenGraphQLFields));
			}
		}

		return graphQLFields;
	}

	protected String[] getIgnoredEntityFieldNames() {
		return new String[0];
	}

	protected boolean equals(
		RecycleBinEntry recycleBinEntry1, RecycleBinEntry recycleBinEntry2) {

		if (recycleBinEntry1 == recycleBinEntry2) {
			return true;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			if (Objects.equals("creator", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						recycleBinEntry1.getCreator(),
						recycleBinEntry2.getCreator())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("dateCreated", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						recycleBinEntry1.getDateCreated(),
						recycleBinEntry2.getDateCreated())) {

					return false;
				}

				continue;
			}

			if (Objects.equals(
					"externalReferenceCode", additionalAssertFieldName)) {

				if (!Objects.deepEquals(
						recycleBinEntry1.getExternalReferenceCode(),
						recycleBinEntry2.getExternalReferenceCode())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("spaceTitle", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						recycleBinEntry1.getSpaceTitle(),
						recycleBinEntry2.getSpaceTitle())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("title", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						recycleBinEntry1.getTitle(),
						recycleBinEntry2.getTitle())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("type", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						recycleBinEntry1.getType(),
						recycleBinEntry2.getType())) {

					return false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		return true;
	}

	protected boolean equals(
		Map<String, Object> map1, Map<String, Object> map2) {

		if (Objects.equals(map1.keySet(), map2.keySet())) {
			for (Map.Entry<String, Object> entry : map1.entrySet()) {
				if (entry.getValue() instanceof Map) {
					if (!equals(
							(Map)entry.getValue(),
							(Map)map2.get(entry.getKey()))) {

						return false;
					}
				}
				else if (!Objects.deepEquals(
							entry.getValue(), map2.get(entry.getKey()))) {

					return false;
				}
			}

			return true;
		}

		return false;
	}

	protected java.lang.reflect.Field[] getDeclaredFields(Class clazz)
		throws Exception {

		if (clazz.getClassLoader() == null) {
			return new java.lang.reflect.Field[0];
		}

		return TransformUtil.transform(
			ReflectionUtil.getDeclaredFields(clazz),
			field -> {
				if (field.isSynthetic()) {
					return null;
				}

				return field;
			},
			java.lang.reflect.Field.class);
	}

	protected java.util.Collection<EntityField> getEntityFields()
		throws Exception {

		if (!(_recycleBinEntryResource instanceof EntityModelResource)) {
			throw new UnsupportedOperationException(
				"Resource is not an instance of EntityModelResource");
		}

		EntityModelResource entityModelResource =
			(EntityModelResource)_recycleBinEntryResource;

		EntityModel entityModel = entityModelResource.getEntityModel(
			new MultivaluedHashMap());

		if (entityModel == null) {
			return Collections.emptyList();
		}

		Map<String, EntityField> entityFieldsMap =
			entityModel.getEntityFieldsMap();

		return entityFieldsMap.values();
	}

	protected List<EntityField> getEntityFields(EntityField.Type type)
		throws Exception {

		return TransformUtil.transform(
			getEntityFields(),
			entityField -> {
				if (!Objects.equals(entityField.getType(), type) ||
					ArrayUtil.contains(
						getIgnoredEntityFieldNames(), entityField.getName())) {

					return null;
				}

				return entityField;
			});
	}

	protected String getFilterString(
		EntityField entityField, String operator,
		RecycleBinEntry recycleBinEntry) {

		StringBundler sb = new StringBundler();

		String entityFieldName = entityField.getName();

		sb.append(entityFieldName);

		sb.append(" ");
		sb.append(operator);
		sb.append(" ");

		if (entityFieldName.equals("creator")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		if (entityFieldName.equals("dateCreated")) {
			if (operator.equals("between")) {
				Date date = recycleBinEntry.getDateCreated();

				sb = new StringBundler();

				sb.append("(");
				sb.append(entityFieldName);
				sb.append(" gt ");
				sb.append(_format.format(date.getTime() - (2 * Time.SECOND)));
				sb.append(" and ");
				sb.append(entityFieldName);
				sb.append(" lt ");
				sb.append(_format.format(date.getTime() + (2 * Time.SECOND)));
				sb.append(")");
			}
			else {
				sb.append(entityFieldName);

				sb.append(" ");
				sb.append(operator);
				sb.append(" ");

				sb.append(_format.format(recycleBinEntry.getDateCreated()));
			}

			return sb.toString();
		}

		if (entityFieldName.equals("externalReferenceCode")) {
			Object object = recycleBinEntry.getExternalReferenceCode();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("spaceTitle")) {
			Object object = recycleBinEntry.getSpaceTitle();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("title")) {
			Object object = recycleBinEntry.getTitle();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("type")) {
			sb.append(String.valueOf(recycleBinEntry.getType()));

			return sb.toString();
		}

		throw new IllegalArgumentException(
			"Invalid entity field " + entityFieldName);
	}

	protected String invoke(String query) throws Exception {
		HttpInvoker httpInvoker = HttpInvoker.newHttpInvoker();

		httpInvoker.body(
			JSONUtil.put(
				"query", query
			).toString(),
			"application/json");
		httpInvoker.httpMethod(HttpInvoker.HttpMethod.POST);
		httpInvoker.path("http://localhost:8080/o/graphql");
		httpInvoker.userNameAndPassword(
			"test@liferay.com:" + PropsValues.DEFAULT_ADMIN_PASSWORD);

		HttpInvoker.HttpResponse httpResponse = httpInvoker.invoke();

		return httpResponse.getContent();
	}

	protected JSONObject invokeGraphQLMutation(GraphQLField graphQLField)
		throws Exception {

		GraphQLField mutationGraphQLField = new GraphQLField(
			"mutation", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(mutationGraphQLField.toString()));
	}

	protected JSONObject invokeGraphQLQuery(GraphQLField graphQLField)
		throws Exception {

		GraphQLField queryGraphQLField = new GraphQLField(
			"query", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(queryGraphQLField.toString()));
	}

	protected RecycleBinEntry randomRecycleBinEntry() throws Exception {
		return new RecycleBinEntry() {
			{
				dateCreated = RandomTestUtil.nextDate();
				externalReferenceCode = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				spaceTitle = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				title = StringUtil.toLowerCase(RandomTestUtil.randomString());
				type = RandomTestUtil.randomInt();
			}
		};
	}

	protected RecycleBinEntry randomIrrelevantRecycleBinEntry()
		throws Exception {

		RecycleBinEntry randomIrrelevantRecycleBinEntry =
			randomRecycleBinEntry();

		return randomIrrelevantRecycleBinEntry;
	}

	protected RecycleBinEntry randomPatchRecycleBinEntry() throws Exception {
		return randomRecycleBinEntry();
	}

	protected RecycleBinEntryResource recycleBinEntryResource;
	protected com.liferay.portal.kernel.model.Group irrelevantGroup;
	protected com.liferay.portal.kernel.model.Company testCompany;
	protected com.liferay.portal.kernel.model.Group testGroup;

	protected static class BeanTestUtil {

		public static void copyProperties(Object source, Object target)
			throws Exception {

			Class<?> sourceClass = source.getClass();

			Class<?> targetClass = target.getClass();

			for (java.lang.reflect.Field field :
					_getAllDeclaredFields(sourceClass)) {

				if (field.isSynthetic()) {
					continue;
				}

				Method getMethod = _getMethod(
					sourceClass, field.getName(), "get");

				try {
					Method setMethod = _getMethod(
						targetClass, field.getName(), "set",
						getMethod.getReturnType());

					setMethod.invoke(target, getMethod.invoke(source));
				}
				catch (Exception e) {
					continue;
				}
			}
		}

		public static boolean hasProperty(Object bean, String name) {
			Method setMethod = _getMethod(
				bean.getClass(), "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod != null) {
				return true;
			}

			return false;
		}

		public static void setProperty(Object bean, String name, Object value)
			throws Exception {

			Class<?> clazz = bean.getClass();

			Method setMethod = _getMethod(
				clazz, "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod == null) {
				throw new NoSuchMethodException();
			}

			Class<?>[] parameterTypes = setMethod.getParameterTypes();

			setMethod.invoke(bean, _translateValue(parameterTypes[0], value));
		}

		private static List<java.lang.reflect.Field> _getAllDeclaredFields(
			Class<?> clazz) {

			List<java.lang.reflect.Field> fields = new ArrayList<>();

			while ((clazz != null) && (clazz != Object.class)) {
				for (java.lang.reflect.Field field :
						clazz.getDeclaredFields()) {

					fields.add(field);
				}

				clazz = clazz.getSuperclass();
			}

			return fields;
		}

		private static Method _getMethod(Class<?> clazz, String name) {
			for (Method method : clazz.getMethods()) {
				if (name.equals(method.getName()) &&
					(method.getParameterCount() == 1) &&
					_parameterTypes.contains(method.getParameterTypes()[0])) {

					return method;
				}
			}

			return null;
		}

		private static Method _getMethod(
				Class<?> clazz, String fieldName, String prefix,
				Class<?>... parameterTypes)
			throws Exception {

			return clazz.getMethod(
				prefix + StringUtil.upperCaseFirstLetter(fieldName),
				parameterTypes);
		}

		private static Object _translateValue(
			Class<?> parameterType, Object value) {

			if ((value instanceof Integer) &&
				parameterType.equals(Long.class)) {

				Integer intValue = (Integer)value;

				return intValue.longValue();
			}

			return value;
		}

		private static final Set<Class<?>> _parameterTypes = new HashSet<>(
			Arrays.asList(
				Boolean.class, Date.class, Double.class, Integer.class,
				Long.class, Map.class, String.class));

	}

	protected class GraphQLField {

		public GraphQLField(String key, GraphQLField... graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(String key, List<GraphQLField> graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			GraphQLField... graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = Arrays.asList(graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			List<GraphQLField> graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = graphQLFields;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(_key);

			if (!_parameterMap.isEmpty()) {
				sb.append("(");

				for (Map.Entry<String, Object> entry :
						_parameterMap.entrySet()) {

					sb.append(entry.getKey());
					sb.append(": ");
					sb.append(entry.getValue());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append(")");
			}

			if (!_graphQLFields.isEmpty()) {
				sb.append("{");

				for (GraphQLField graphQLField : _graphQLFields) {
					sb.append(graphQLField.toString());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append("}");
			}

			return sb.toString();
		}

		private final List<GraphQLField> _graphQLFields;
		private final String _key;
		private final Map<String, Object> _parameterMap;

	}

	private static final com.liferay.portal.kernel.log.Log _log =
		LogFactoryUtil.getLog(BaseRecycleBinEntryResourceTestCase.class);

	private static Format _format;

	private com.liferay.portal.kernel.model.User _testCompanyAdminUser;

	@Inject
	private com.liferay.trash.rest.resource.v1_0.RecycleBinEntryResource
		_recycleBinEntryResource;

}