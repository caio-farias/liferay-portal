/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.events;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.security.Provider;
import java.security.Security;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Exercises {@link FIPSComplianceValidator} against the real
 * {@link java.security.Security} provider set using stub {@link Provider}
 * instances. The assertions target the integrity invariant — whether the
 * validator halts startup via {@link ActionException} — not the emitted
 * audit line.
 *
 * @author Caio Farias
 */
public class FIPSComplianceValidatorTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_originalProviders = Security.getProviders();

		_originalFIPSEnabled = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED);
		_originalProviderName = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_PROVIDER_NAME);
		_originalProviderApproved = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_PROVIDER_APPROVED);
		_originalProviderStrict = ReflectionTestUtil.getFieldValue(
			PropsValues.class, _FIELD_PROVIDER_STRICT);

		for (Provider provider : _originalProviders) {
			Security.removeProvider(provider.getName());
		}

		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_NAME, _FIPS_PROVIDER_NAME);
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_APPROVED,
			new String[] {_FIPS_PROVIDER_NAME, "BCJSSE"});
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_STRICT, true);
	}

	@After
	public void tearDown() {
		for (Provider provider : Security.getProviders()) {
			Security.removeProvider(provider.getName());
		}

		for (Provider provider : _originalProviders) {
			Security.addProvider(provider);
		}

		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, _originalFIPSEnabled);
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_NAME, _originalProviderName);
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_APPROVED,
			_originalProviderApproved);
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_STRICT, _originalProviderStrict);
	}

	@Test
	public void testAllowsUnapprovedProviderWhenStrictOff() throws Exception {
		_enableFIPS();

		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_PROVIDER_STRICT, false);

		Security.insertProviderAt(new _StubProvider(_FIPS_PROVIDER_NAME), 1);
		Security.addProvider(new _StubProvider("Rogue"));

		new FIPSComplianceValidator().run(null);
	}

	@Test(expected = ActionException.class)
	public void testFailsWhenFIPSProviderAbsent() throws Exception {
		_enableFIPS();

		Security.addProvider(new _StubProvider("SomethingElse"));

		new FIPSComplianceValidator().run(null);
	}

	@Test(expected = ActionException.class)
	public void testFailsWhenFIPSProviderNotFirst() throws Exception {
		_enableFIPS();

		Security.insertProviderAt(new _StubProvider("BCJSSE"), 1);
		Security.insertProviderAt(new _StubProvider(_FIPS_PROVIDER_NAME), 2);

		new FIPSComplianceValidator().run(null);
	}

	@Test(expected = ActionException.class)
	public void testFailsWhenNoProvidersRegistered() throws Exception {
		_enableFIPS();

		new FIPSComplianceValidator().run(null);
	}

	@Test(expected = ActionException.class)
	public void testFailsWhenUnapprovedProviderPresentStrict() throws Exception {
		_enableFIPS();

		Security.insertProviderAt(new _StubProvider(_FIPS_PROVIDER_NAME), 1);
		Security.addProvider(new _StubProvider("Rogue"));

		new FIPSComplianceValidator().run(null);
	}

	@Test
	public void testNoOpWhenFIPSDisabled() throws Exception {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, false);

		Security.addProvider(new _StubProvider("DeliberatelyUnapproved"));

		new FIPSComplianceValidator().run(null);
	}

	@Test
	public void testPassesWhenFIPSProviderIsFirstAndOnlyApproved()
		throws Exception {

		_enableFIPS();

		Security.insertProviderAt(new _StubProvider(_FIPS_PROVIDER_NAME), 1);
		Security.insertProviderAt(new _StubProvider("BCJSSE"), 2);

		new FIPSComplianceValidator().run(null);
	}

	private void _enableFIPS() {
		ReflectionTestUtil.setFieldValue(
			PropsValues.class, _FIELD_FIPS_MODE_ENABLED, true);
	}

	private static final String _FIELD_FIPS_MODE_ENABLED =
		"PORTAL_SECURITY_FIPS_MODE_ENABLED";

	private static final String _FIELD_PROVIDER_APPROVED =
		"PORTAL_SECURITY_FIPS_PROVIDER_APPROVED";

	private static final String _FIELD_PROVIDER_NAME =
		"PORTAL_SECURITY_FIPS_PROVIDER_NAME";

	private static final String _FIELD_PROVIDER_STRICT =
		"PORTAL_SECURITY_FIPS_PROVIDER_STRICT";

	private static final String _FIPS_PROVIDER_NAME = "BCFIPS";

	private boolean _originalFIPSEnabled;
	private String[] _originalProviderApproved;
	private String _originalProviderName;
	private boolean _originalProviderStrict;
	private Provider[] _originalProviders;

	private static class _StubProvider extends Provider {

		private _StubProvider(String name) {
			super(name, 1.0, "FIPSComplianceValidator test stub");
		}

	}

}
