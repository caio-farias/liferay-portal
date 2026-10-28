/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.lang.reflect.Field;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CBOMBuilderTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(new JSONFactoryImpl());
	}

	@Test
	public void testGetCryptoUses() throws Exception {
		Set<String> propsKeys = new HashSet<>();

		for (Field field : PropsKeys.class.getFields()) {
			propsKeys.add((String)field.get(null));
		}

		Map<String, List<String>> configuredByMap = new TreeMap<>();

		for (CryptoUse cryptoUse : CBOMBuilder.getCryptoUses()) {
			List<String> classNames = cryptoUse.getClassNames();

			Assert.assertFalse(classNames.isEmpty());

			List<CryptoFunction> cryptoFunctions =
				cryptoUse.getCryptoFunctions();

			Assert.assertFalse(cryptoFunctions.isEmpty());

			List<String> jcaClassNames = cryptoUse.getJCAClassNames();

			Assert.assertFalse(jcaClassNames.isEmpty());

			if (cryptoUse.isConfigurable()) {
				Assert.assertNull(cryptoUse.getAlgorithm());

				List<String> configuredBy = cryptoUse.getConfiguredBy();

				Assert.assertTrue(
					configuredBy.toString(),
					propsKeys.containsAll(configuredBy));

				configuredByMap.put(cryptoUse.getFeatureName(), configuredBy);

				continue;
			}

			Assert.assertEquals(
				jcaClassNames.toString(), 1, jcaClassNames.size());

			CryptoProviderUtil.getProviderName(
				cryptoUse.getAlgorithm(), jcaClassNames.get(0));
		}

		Assert.assertEquals(
			HashMapBuilder.<String, List<String>>put(
				"Authentication MAC",
				Collections.singletonList("auth.mac.algorithm")
			).put(
				"Company encryption",
				Arrays.asList(
					"company.encryption.algorithm",
					"company.encryption.key.size")
			).put(
				"Encrypted object fields",
				Collections.singletonList("object.encryption.algorithm")
			).put(
				"Legacy password verification",
				Collections.singletonList(
					"passwords.encryption.algorithm.legacy")
			).put(
				"Password hashing",
				Collections.singletonList("passwords.encryption.algorithm")
			).put(
				"Tunnel authentication",
				Collections.singletonList(
					"tunneling.servlet.encryption.algorithm")
			).build(),
			configuredByMap);
	}

}