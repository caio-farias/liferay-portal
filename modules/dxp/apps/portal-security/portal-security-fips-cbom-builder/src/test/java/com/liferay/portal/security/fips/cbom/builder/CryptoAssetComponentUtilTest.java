/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Manuele Castro
 */
public class CryptoAssetComponentUtilTest {

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
	public void testToComponents() throws Exception {
		CryptoUse cryptoUse1 = new CryptoUse(
			"SHA-256",
			Arrays.asList("com.liferay.test.Class2", "com.liferay.test.Class1"),
			Collections.emptyList(),
			Collections.singletonList(CryptoFunction.DIGEST), "Feature B",
			Collections.singletonList("MessageDigest"), Primitive.HASH);
		CryptoUse cryptoUse2 = new CryptoUse(
			"SHA-256",
			Arrays.asList("com.liferay.test.Class3", "com.liferay.test.Class2"),
			Collections.emptyList(),
			Collections.singletonList(CryptoFunction.DIGEST), "Feature A",
			Collections.singletonList("MessageDigest"), Primitive.HASH);
		CryptoUse cryptoUse3 = new CryptoUse(
			null, Collections.singletonList("com.liferay.test.Class4"),
			Arrays.asList("test.algorithm", "test.key.size"),
			Arrays.asList(
				CryptoFunction.ENCRYPT, CryptoFunction.DECRYPT,
				CryptoFunction.KEYGEN),
			"Feature C", Arrays.asList("Cipher", "KeyGenerator"),
			Primitive.BLOCK_CIPHER);

		List<CryptoUse> cryptoUses = Arrays.asList(
			cryptoUse1, cryptoUse2, cryptoUse3);

		Properties portalProperties = new Properties();

		portalProperties.setProperty("test.algorithm", "AES");

		List<Component> components = CryptoAssetComponentUtil.toComponents(
			cryptoUses, portalProperties);

		Bom bom = new Bom();

		bom.setComponents(components);

		BomJsonGenerator bomJsonGenerator = BomGeneratorFactory.createJson(
			Version.VERSION_16, bom);

		String json = bomJsonGenerator.toJsonString();

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(json);

		JSONArray componentsJSONArray = jsonObject.getJSONArray("components");

		JSONAssert.assertEquals(
			JSONUtil.putAll(
				JSONUtil.put(
					"bom-ref", "crypto:Cipher:test.algorithm"
				).put(
					"cryptoProperties",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions",
							JSONUtil.putAll("keygen", "encrypt", "decrypt")
						).put(
							"primitive", "block-cipher"
						)
					).put(
						"assetType", "algorithm"
					)
				).put(
					"evidence",
					JSONUtil.put(
						"occurrences",
						JSONUtil.putAll(
							JSONUtil.put(
								"location", "com.liferay.test.Class4")))
				).put(
					"name", "test.algorithm"
				).put(
					"properties",
					JSONUtil.putAll(
						JSONUtil.put(
							"name", "liferay:cbom:configured-by"
						).put(
							"value", "test.algorithm"
						),
						JSONUtil.put(
							"name", "liferay:cbom:configured-by"
						).put(
							"value", "test.key.size"
						),
						JSONUtil.put(
							"name", "liferay:cbom:default-value"
						).put(
							"value", "test.algorithm=AES"
						),
						JSONUtil.put(
							"name", "liferay:cbom:default-value"
						).put(
							"value", "test.key.size="
						),
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature C"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "Cipher"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "KeyGenerator"
						))
				).put(
					"type", "cryptographic-asset"
				),
				JSONUtil.put(
					"bom-ref", "crypto:MessageDigest:SHA-256"
				).put(
					"cryptoProperties",
					JSONUtil.put(
						"algorithmProperties",
						JSONUtil.put(
							"cryptoFunctions", JSONUtil.putAll("digest")
						).put(
							"primitive", "hash"
						)
					).put(
						"assetType", "algorithm"
					)
				).put(
					"evidence",
					JSONUtil.put(
						"occurrences",
						JSONUtil.putAll(
							JSONUtil.put("location", "com.liferay.test.Class1"),
							JSONUtil.put("location", "com.liferay.test.Class2"),
							JSONUtil.put(
								"location", "com.liferay.test.Class3")))
				).put(
					"name", "SHA-256"
				).put(
					"properties",
					JSONUtil.putAll(
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature A"
						),
						JSONUtil.put(
							"name", "liferay:cbom:feature"
						).put(
							"value", "Feature B"
						),
						JSONUtil.put(
							"name", "liferay:cbom:jca-class"
						).put(
							"value", "MessageDigest"
						),
						JSONUtil.put(
							"name", "liferay:cbom:provider"
						).put(
							"value", "SUN"
						))
				).put(
					"type", "cryptographic-asset"
				)
			).toString(),
			componentsJSONArray.toString(), JSONCompareMode.STRICT);

		CryptoUse cryptoUse4 = new CryptoUse(
			null, Collections.singletonList("com.liferay.test.Class5"),
			Collections.singletonList("test.algorithm"),
			Collections.singletonList(CryptoFunction.ENCRYPT), "Feature D",
			Collections.singletonList("Cipher"), Primitive.BLOCK_CIPHER);

		cryptoUses = Arrays.asList(cryptoUse3, cryptoUse4);

		try {
			CryptoAssetComponentUtil.toComponents(cryptoUses, portalProperties);

			Assert.fail();
		}
		catch (IllegalArgumentException illegalArgumentException) {
			Assert.assertEquals(
				"Unable to merge the Feature C and Feature D uses of " +
					"crypto:Cipher:test.algorithm",
				illegalArgumentException.getMessage());
		}
	}

}