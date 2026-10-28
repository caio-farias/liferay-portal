/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Manuele Castro
 */
public class CryptoProviderUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetProviderName() throws Exception {
		Assert.assertEquals(
			"SUN",
			CryptoProviderUtil.getProviderName("SHA-256", "MessageDigest"));

		try {
			CryptoProviderUtil.getProviderName(
				RandomTestUtil.randomString(), "MessageDigest");

			Assert.fail();
		}
		catch (NoSuchAlgorithmException noSuchAlgorithmException) {
		}

		String jcaClassName = RandomTestUtil.randomString();

		try {
			CryptoProviderUtil.getProviderName("SHA-256", jcaClassName);

			Assert.fail();
		}
		catch (IllegalArgumentException illegalArgumentException) {
			Assert.assertEquals(
				"Unsupported JCA class " + jcaClassName,
				illegalArgumentException.getMessage());
		}
	}

}