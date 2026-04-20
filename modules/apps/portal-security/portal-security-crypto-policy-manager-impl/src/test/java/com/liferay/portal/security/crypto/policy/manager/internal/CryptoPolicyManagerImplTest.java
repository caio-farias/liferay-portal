/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.manager.internal;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.security.crypto.policy.manager.CryptoUseCase;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.security.Security;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Exercises {@link CryptoPolicyManagerImpl} against the real JVM
 * {@link java.security.Security} property store. Assertions target the
 * denylist invariant — whether a (useCase, algorithm, keySize) is reported as
 * allowed by the merged JVM disabled-algorithms configuration — not the
 * parser's field layout or any plumbing shape.
 *
 * @author Caio Farias
 */
public class CryptoPolicyManagerImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_originalFIPSModeEnabled = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED);

		for (String propertyName : _JVM_PROPERTIES) {
			_originalPropertyValues.put(
				propertyName, Security.getProperty(propertyName));

			Security.setProperty(propertyName, "");
		}
	}

	@After
	public void tearDown() {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED,
			_originalFIPSModeEnabled);

		for (Map.Entry<String, String> entry :
				_originalPropertyValues.entrySet()) {

			String originalValue = entry.getValue();

			Security.setProperty(
				entry.getKey(), (originalValue == null) ? "" : originalValue);
		}
	}

	@Test
	public void testAlgorithmNotInDenylistIsAllowed() {
		_setFIPSEnabled(true);

		Security.setProperty("jdk.certpath.disabledAlgorithms", "MD5, SHA1");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertTrue(
			impl.isAlgorithmAllowed(CryptoUseCase.KEY_MANAGEMENT, "AES"));
		Assert.assertTrue(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "AES", 128));
	}

	@Test
	public void testCaseInsensitiveAlgorithmMatch() {
		_setFIPSEnabled(true);

		Security.setProperty("jdk.certpath.disabledAlgorithms", "MD5");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "MD5"));
		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "md5"));
		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "Md5"));
	}

	@Test
	public void testComplexJVMSyntaxEntriesAreSkipped() {
		_setFIPSEnabled(true);

		Security.setProperty(
			"jdk.certpath.disabledAlgorithms",
			"SHA1 jdkCA & usage TLSServer, " +
				"SHA1 usage SignedJAR & denyAfter 2019-01-01, " +
					"RSA keySize <= 1024, " +
						"MD5");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertFalse(
			"Simple entry must still parse",
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "MD5"));

		Assert.assertTrue(
			"SHA1 with jdkCA/usage/denyAfter clauses must be skipped",
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "SHA1"));

		Assert.assertTrue(
			"RSA with unsupported '<=' operator must be skipped",
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 1024));
	}

	@Test
	public void testConflictingEntriesTakeTheMostRestrictive() {
		_setFIPSEnabled(true);

		Security.setProperty(
			"jdk.certpath.disabledAlgorithms", "RSA keySize < 1024");
		Security.setProperty(
			"jdk.tls.disabledAlgorithms", "RSA keySize < 2048");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertFalse(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 1024));
		Assert.assertFalse(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 2047));
		Assert.assertTrue(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 2048));
	}

	@Test
	public void testEmptyPropertyValueIsTolerated() {
		_setFIPSEnabled(true);

		Security.setProperty("jdk.certpath.disabledAlgorithms", "");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertTrue(
			impl.isAlgorithmAllowed(CryptoUseCase.KEY_MANAGEMENT, "AES"));
	}

	@Test
	public void testIsApprovedModeReflectsFIPSMode() {
		_setFIPSEnabled(false);

		CryptoPolicyManagerImpl disabledImpl = _newActivatedImpl();

		Assert.assertFalse(disabledImpl.isApprovedMode());

		_setFIPSEnabled(true);

		CryptoPolicyManagerImpl enabledImpl = _newActivatedImpl();

		Assert.assertTrue(enabledImpl.isApprovedMode());
	}

	@Test
	public void testKeySizeBelowMinimumIsRejected() {
		_setFIPSEnabled(true);

		Security.setProperty(
			"jdk.certpath.disabledAlgorithms", "RSA keySize < 2048");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertTrue(
			"Algorithm itself is not unconditionally disabled",
			impl.isAlgorithmAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA"));

		Assert.assertFalse(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 1024));
		Assert.assertFalse(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 2047));
		Assert.assertTrue(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 2048));
		Assert.assertTrue(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 4096));
	}

	@Test
	public void testPassthroughWhenFIPSDisabled() {
		_setFIPSEnabled(false);

		Security.setProperty(
			"jdk.certpath.disabledAlgorithms",
			"MD5, SHA1, RSA keySize < 2048");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertTrue(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "MD5"));
		Assert.assertTrue(
			impl.isKeySizeAllowed(CryptoUseCase.KEY_MANAGEMENT, "RSA", 1024));
	}

	@Test
	public void testUnconditionallyDisabledAlgorithmRejectedAtEverySize() {
		_setFIPSEnabled(true);

		Security.setProperty("jdk.certpath.disabledAlgorithms", "MD5");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "MD5"));
		Assert.assertFalse(
			impl.isKeySizeAllowed(CryptoUseCase.DATA_PROTECTION, "MD5", 128));
		Assert.assertFalse(
			impl.isKeySizeAllowed(
				CryptoUseCase.DATA_PROTECTION, "MD5", Integer.MAX_VALUE - 1));
	}

	@Test
	public void testUnionAcrossAllConfiguredJVMProperties() {
		_setFIPSEnabled(true);

		Security.setProperty("jdk.certpath.disabledAlgorithms", "MD5");
		Security.setProperty("jdk.jar.disabledAlgorithms", "SHA1");
		Security.setProperty("jdk.security.legacyAlgorithms", "DES");
		Security.setProperty("jdk.tls.disabledAlgorithms", "RC4");
		Security.setProperty(
			"http.auth.digest.disabledAlgorithms", "MD2");

		CryptoPolicyManagerImpl impl = _newActivatedImpl();

		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "MD5"));
		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "SHA1"));
		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.KEY_MANAGEMENT, "DES"));
		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "RC4"));
		Assert.assertFalse(
			impl.isAlgorithmAllowed(CryptoUseCase.DATA_PROTECTION, "MD2"));
	}

	private CryptoPolicyManagerImpl _newActivatedImpl() {
		CryptoPolicyManagerImpl impl = new CryptoPolicyManagerImpl();

		ReflectionTestUtil.invoke(
			impl, "activate", new Class<?>[0], new Object[0]);

		return impl;
	}

	private void _setFIPSEnabled(boolean enabled) {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, enabled);
	}

	private static final String _FIELD_FIPS_MODE_ENABLED =
		"PORTAL_SECURITY_FIPS_MODE_ENABLED";

	private static final String[] _JVM_PROPERTIES = {
		"http.auth.digest.disabledAlgorithms",
		"jdk.certpath.disabledAlgorithms", "jdk.jar.disabledAlgorithms",
		"jdk.security.legacyAlgorithms", "jdk.tls.disabledAlgorithms"
	};

	private boolean _originalFIPSModeEnabled;
	private final Map<String, String> _originalPropertyValues = new HashMap<>();

}
