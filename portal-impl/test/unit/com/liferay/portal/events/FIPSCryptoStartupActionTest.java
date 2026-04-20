/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.events;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Exercises {@link FIPSCryptoStartupAction} against the real JCE
 * {@code MessageDigest}, {@code Mac}, and {@code Cipher} engines. The assertions
 * target the integrity invariant — that a tampered KAT expected-value causes the
 * action to halt startup via {@link ActionException} rather than silently pass.
 *
 * @author Caio Farias
 */
public class FIPSCryptoStartupActionTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_originalFIPSEnabled = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED);

		_originalSHA256Digest = ReflectionTestUtil.getFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_SHA_256_DIGEST);
		_originalHmacSHA256Mac = ReflectionTestUtil.getFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_HMAC_SHA_256_MAC);
	}

	@After
	public void tearDown() {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, _originalFIPSEnabled);

		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_SHA_256_DIGEST,
			_originalSHA256Digest);
		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_HMAC_SHA_256_MAC,
			_originalHmacSHA256Mac);
	}

	@Test(expected = ActionException.class)
	public void testFailsWhenHmacSHA256ExpectedMACTampered() throws Exception {
		_enableFIPS();

		byte[] tampered = ((byte[])_originalHmacSHA256Mac).clone();

		tampered[0] = (byte)(tampered[0] ^ 0x01);

		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_HMAC_SHA_256_MAC, tampered);

		new FIPSCryptoStartupAction().run(null);
	}

	@Test(expected = ActionException.class)
	public void testFailsWhenSHA256ExpectedDigestTampered() throws Exception {
		_enableFIPS();

		byte[] tampered = ((byte[])_originalSHA256Digest).clone();

		tampered[0] = (byte)(tampered[0] ^ 0x01);

		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_SHA_256_DIGEST, tampered);

		new FIPSCryptoStartupAction().run(null);
	}

	@Test
	public void testFirstKATFailureAbortsRemainingKATs() throws Exception {
		_enableFIPS();

		byte[] tamperedSHA = ((byte[])_originalSHA256Digest).clone();

		tamperedSHA[0] = (byte)(tamperedSHA[0] ^ 0x01);

		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_SHA_256_DIGEST, tamperedSHA);

		byte[] tamperedHmac = ((byte[])_originalHmacSHA256Mac).clone();

		tamperedHmac[0] = (byte)(tamperedHmac[0] ^ 0x01);

		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_HMAC_SHA_256_MAC,
			tamperedHmac);

		try {
			new FIPSCryptoStartupAction().run(null);

			Assert.fail("Expected ActionException for tampered SHA-256 KAT");
		}
		catch (ActionException actionException) {
			Assert.assertTrue(
				actionException.getMessage(),
				actionException.getMessage().contains("kat.sha256"));
			Assert.assertFalse(
				actionException.getMessage(),
				actionException.getMessage().contains("hmacSha256"));
		}
	}

	@Test
	public void testNoOpWhenFIPSDisabled() throws Exception {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, false);

		byte[] tampered = ((byte[])_originalSHA256Digest).clone();

		tampered[0] = (byte)(tampered[0] ^ 0x01);

		ReflectionTestUtil.setFieldValue(
			FIPSCryptoStartupAction.class, _FIELD_SHA_256_DIGEST, tampered);

		new FIPSCryptoStartupAction().run(null);
	}

	@Test
	public void testPassesWhenAllKATsMatchCanonicalValues() throws Exception {
		_enableFIPS();

		new FIPSCryptoStartupAction().run(null);
	}

	private void _enableFIPS() {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, true);
	}

	private static final String _FIELD_FIPS_MODE_ENABLED =
		"PORTAL_SECURITY_FIPS_MODE_ENABLED";

	private static final String _FIELD_HMAC_SHA_256_MAC =
		"_HMAC_SHA_256_EXPECTED_MAC";

	private static final String _FIELD_SHA_256_DIGEST =
		"_SHA_256_EMPTY_INPUT_EXPECTED_DIGEST";

	private boolean _originalFIPSEnabled;
	private Object _originalHmacSHA256Mac;
	private Object _originalSHA256Digest;

}
