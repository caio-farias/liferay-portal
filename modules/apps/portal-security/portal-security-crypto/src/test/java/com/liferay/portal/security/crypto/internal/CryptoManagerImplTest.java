/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.internal;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.security.crypto.policy.manager.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.manager.CryptoUseCase;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Exercises {@link CryptoManagerImpl} keygen routing. Policy layer is
 * isolated with a fake {@link CryptoPolicyManager}; JCE layer uses real JVM
 * providers (SunJCE for symmetric, SunRsaSign for RSA key pairs) so the
 * assertions target the routing invariant — whether the manager halts on
 * policy rejection, succeeds through a real provider that supports the
 * algorithm, and fails through one that does not.
 *
 * @author Caio Farias
 */
public class CryptoManagerImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_originalFIPSModeEnabled = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED);
		_originalFIPSProviderName = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_FIPS_PROVIDER_NAME);
	}

	@After
	public void tearDown() {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED,
			_originalFIPSModeEnabled);
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_PROVIDER_NAME,
			_originalFIPSProviderName);
	}

	@Test(expected = SecurityException.class)
	public void testGenerateKeyFailsWhenFIPSProviderDoesNotSupportAlgorithm() {
		_setFIPSEnabled(true);
		_setFIPSProviderName("SunRsaSign");

		CryptoManagerImpl manager = _newManager(_alwaysAllow());

		manager.generateKey(
			CryptoUseCase.KEY_MANAGEMENT, "AES", 128,
			"test.noSuchAlgorithmAtProvider");
	}

	@Test(expected = SecurityException.class)
	public void testGenerateKeyHaltsWhenPolicyRejects() {
		_setFIPSEnabled(true);
		_setFIPSProviderName("SunJCE");

		CryptoManagerImpl manager = _newManager(_alwaysDeny());

		manager.generateKey(
			CryptoUseCase.KEY_MANAGEMENT, "AES", 128, "test.policyReject");
	}

	@Test(expected = SecurityException.class)
	public void testGenerateKeyPairHaltsWhenPolicyRejects() {
		_setFIPSEnabled(true);
		_setFIPSProviderName("SunRsaSign");

		CryptoManagerImpl manager = _newManager(_alwaysDeny());

		manager.generateKeyPair(
			CryptoUseCase.KEY_MANAGEMENT, "RSA", 2048, "test.policyReject");
	}

	@Test
	public void testGenerateKeyPairRoutedThroughFIPSProviderWhenEnabled() {
		_setFIPSEnabled(true);
		_setFIPSProviderName("SunRsaSign");

		CryptoManagerImpl manager = _newManager(_alwaysAllow());

		KeyPair keyPair = manager.generateKeyPair(
			CryptoUseCase.KEY_MANAGEMENT, "RSA", 2048,
			"test.generateKeyPairFIPSOn");

		Assert.assertNotNull(keyPair);
		Assert.assertNotNull(keyPair.getPublic());
		Assert.assertNotNull(keyPair.getPrivate());
		Assert.assertEquals("RSA", keyPair.getPublic().getAlgorithm());
	}

	@Test
	public void testGenerateKeyPassesThroughWhenFIPSDisabled() {
		_setFIPSEnabled(false);

		CryptoManagerImpl manager = _newManager(_alwaysDeny());

		SecretKey secretKey = manager.generateKey(
			CryptoUseCase.KEY_MANAGEMENT, "AES", 128,
			"test.generateKeyFIPSOff");

		Assert.assertNotNull(secretKey);
		Assert.assertEquals("AES", secretKey.getAlgorithm());
	}

	@Test
	public void testGenerateKeyRoutedThroughFIPSProviderWhenEnabled() {
		_setFIPSEnabled(true);
		_setFIPSProviderName("SunJCE");

		CryptoManagerImpl manager = _newManager(_alwaysAllow());

		SecretKey secretKey = manager.generateKey(
			CryptoUseCase.KEY_MANAGEMENT, "AES", 128,
			"test.generateKeyFIPSOn");

		Assert.assertNotNull(secretKey);
		Assert.assertEquals("AES", secretKey.getAlgorithm());
	}

	private CryptoPolicyManager _alwaysAllow() {
		return new CryptoPolicyManager() {

			@Override
			public boolean isAlgorithmAllowed(
				CryptoUseCase cryptoUseCase, String algorithm) {

				return true;
			}

			@Override
			public boolean isApprovedMode() {
				return true;
			}

			@Override
			public boolean isKeySizeAllowed(
				CryptoUseCase cryptoUseCase, String algorithm, int keySize) {

				return true;
			}

		};
	}

	private CryptoPolicyManager _alwaysDeny() {
		return new CryptoPolicyManager() {

			@Override
			public boolean isAlgorithmAllowed(
				CryptoUseCase cryptoUseCase, String algorithm) {

				return false;
			}

			@Override
			public boolean isApprovedMode() {
				return true;
			}

			@Override
			public boolean isKeySizeAllowed(
				CryptoUseCase cryptoUseCase, String algorithm, int keySize) {

				return false;
			}

		};
	}

	private CryptoManagerImpl _newManager(CryptoPolicyManager policy) {
		CryptoManagerImpl impl = new CryptoManagerImpl();

		ReflectionTestUtil.setFieldValue(impl, "_cryptoPolicyManager", policy);

		return impl;
	}

	private void _setFIPSEnabled(boolean enabled) {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, enabled);
	}

	private void _setFIPSProviderName(String providerName) {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_PROVIDER_NAME, providerName);
	}

	private static final String _FIELD_FIPS_MODE_ENABLED =
		"PORTAL_SECURITY_FIPS_MODE_ENABLED";

	private static final String _FIELD_FIPS_PROVIDER_NAME =
		"PORTAL_SECURITY_FIPS_PROVIDER_NAME";

	private boolean _originalFIPSModeEnabled;
	private String _originalFIPSProviderName;

}
