/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.events;

import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.security.fips.FIPSAuditLog;
import com.liferay.portal.kernel.security.fips.FIPSModeUtil;

import java.security.Provider;
import java.security.Security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verifies at global startup that the JVM's JCE provider set is FIPS 140-3
 * compliant when FIPS mode is enabled. The configured FIPS provider must be
 * the first registered JCE provider, and when strict mode is on no unapproved
 * providers may be registered. Any violation emits a critical audit line and
 * aborts startup by throwing {@link ActionException}.
 *
 * @author Caio Farias
 */
public class FIPSComplianceValidator extends SimpleAction {

	@Override
	public void run(String[] ids) throws ActionException {
		if (!FIPSModeUtil.isFIPSModeEnabled()) {
			return;
		}

		_validateFIPSProvider();
		_validateApprovedProviders();
	}

	private List<String> _collectProviderNames() {
		Provider[] providers = Security.getProviders();

		List<String> names = new ArrayList<>(providers.length);

		for (Provider provider : providers) {
			names.add(provider.getName());
		}

		return names;
	}

	private void _validateApprovedProviders() throws ActionException {
		Set<String> approved = new LinkedHashSet<>(
			Arrays.asList(FIPSModeUtil.getApprovedProviderNames()));

		List<String> unapproved = new ArrayList<>();

		for (Provider provider : Security.getProviders()) {
			if (!approved.contains(provider.getName())) {
				unapproved.add(provider.getName());
			}
		}

		if (unapproved.isEmpty()) {
			return;
		}

		Map<String, Object> details = new LinkedHashMap<>();

		details.put("approved", approved);
		details.put("unapproved", unapproved);

		if (FIPSModeUtil.isProviderStrict()) {
			FIPSAuditLog.logCritical(
				_OPERATION_VALIDATE_APPROVED_PROVIDERS,
				_FEATURE_APPROVED_LIST, details);

			throw new ActionException(
				"Unapproved JCE providers registered in FIPS mode: " +
					unapproved);
		}

		FIPSAuditLog.logWarn(
			_OPERATION_VALIDATE_APPROVED_PROVIDERS, _FEATURE_APPROVED_LIST,
			details);
	}

	private void _validateFIPSProvider() throws ActionException {
		String fipsProviderName = FIPSModeUtil.getFIPSProviderName();

		Provider[] providers = Security.getProviders();

		if ((providers.length > 0) &&
			fipsProviderName.equals(providers[0].getName())) {

			return;
		}

		Map<String, Object> details = new LinkedHashMap<>();

		details.put("expectedProvider", fipsProviderName);
		details.put("registeredProviders", _collectProviderNames());

		FIPSAuditLog.logCritical(
			_OPERATION_VALIDATE_FIPS_PROVIDER, _FEATURE_FIPS_PROVIDER, details);

		throw new ActionException(
			"FIPS provider \"" + fipsProviderName +
				"\" must be the first registered JCE provider");
	}

	private static final String _FEATURE_APPROVED_LIST =
		"JCE provider approved-list";

	private static final String _FEATURE_FIPS_PROVIDER = "JCE FIPS provider";

	private static final String _OPERATION_VALIDATE_APPROVED_PROVIDERS =
		"validateApprovedProviders";

	private static final String _OPERATION_VALIDATE_FIPS_PROVIDER =
		"validateFIPSProvider";

}
