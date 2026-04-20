/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.manager.internal;

import com.liferay.portal.kernel.security.fips.FIPSModeUtil;
import com.liferay.portal.security.crypto.policy.manager.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.manager.CryptoUseCase;

import java.security.Security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Reads the JVM's standard disabled-algorithms security properties at
 * activation, merges their entries into a single
 * {@code algorithm -> minimum-disabled-key-size} map, and enforces that map
 * when {@link FIPSModeUtil#isFIPSModeEnabled} is true. The sentinel
 * {@link Integer#MAX_VALUE} represents an unconditional rejection (entries
 * without a {@code keySize} clause). The properties consulted are:
 * <ul>
 * <li>{@code http.auth.digest.disabledAlgorithms}</li>
 * <li>{@code jdk.certpath.disabledAlgorithms}</li>
 * <li>{@code jdk.jar.disabledAlgorithms}</li>
 * <li>{@code jdk.security.legacyAlgorithms}</li>
 * <li>{@code jdk.tls.disabledAlgorithms}</li>
 * </ul>
 *
 * <p>
 * Supports the JVM syntax subset: {@code NAME} (unconditional) and
 * {@code NAME keySize < INT}. Entries using other clauses ({@code usage},
 * {@code denyAfter}, {@code jdkCA}, {@code &} conjunctions, comparison
 * operators other than {@code <}) are skipped because their scope does not
 * apply to keygen policy and a partial parse would produce false rejections.
 * {@code jdk.xml.dsig.secureValidationPolicy} uses a different syntax and is
 * not read here.
 * </p>
 *
 * @author Caio Farias
 */
@Component(service = CryptoPolicyManager.class)
public class CryptoPolicyManagerImpl implements CryptoPolicyManager {

	@Override
	public boolean isAlgorithmAllowed(
		CryptoUseCase cryptoUseCase, String algorithm) {

		if (!isApprovedMode()) {
			return true;
		}

		Integer minDisabledKeySize = _disabledMinKeySizes.get(
			algorithm.toLowerCase(Locale.ROOT));

		if (minDisabledKeySize == null) {
			return true;
		}

		return minDisabledKeySize != Integer.MAX_VALUE;
	}

	@Override
	public boolean isApprovedMode() {
		return FIPSModeUtil.isFIPSModeEnabled();
	}

	@Override
	public boolean isKeySizeAllowed(
		CryptoUseCase cryptoUseCase, String algorithm, int keySize) {

		if (!isApprovedMode()) {
			return true;
		}

		Integer minDisabledKeySize = _disabledMinKeySizes.get(
			algorithm.toLowerCase(Locale.ROOT));

		if (minDisabledKeySize == null) {
			return true;
		}

		return keySize >= minDisabledKeySize;
	}

	@Activate
	protected void activate() {
		Map<String, Integer> map = new HashMap<>();

		for (String propertyName : _JVM_DISABLED_ALGORITHM_PROPERTIES) {
			String propertyValue = Security.getProperty(propertyName);

			if ((propertyValue == null) || propertyValue.isBlank()) {
				continue;
			}

			for (String rawEntry : propertyValue.split(",")) {
				_parseInto(rawEntry.trim(), map);
			}
		}

		_disabledMinKeySizes = Collections.unmodifiableMap(map);
	}

	private void _parseInto(String rawEntry, Map<String, Integer> map) {
		if (rawEntry.isEmpty() || rawEntry.contains("&") ||
			rawEntry.contains("usage") || rawEntry.contains("denyAfter") ||
			rawEntry.contains("jdkCA")) {

			return;
		}

		String lowercased = rawEntry.toLowerCase(Locale.ROOT);

		int keySizeIndex = lowercased.indexOf("keysize");

		if (keySizeIndex < 0) {
			map.merge(lowercased, Integer.MAX_VALUE, Math::max);

			return;
		}

		String name = lowercased.substring(0, keySizeIndex).trim();

		String rest = lowercased.substring(
			keySizeIndex + "keysize".length()
		).trim();

		if (!rest.startsWith("<") || rest.startsWith("<=")) {
			return;
		}

		try {
			int minKeySize = Integer.parseInt(rest.substring(1).trim());

			map.merge(name, minKeySize, Math::max);
		}
		catch (NumberFormatException numberFormatException) {
		}
	}

	private static final String[] _JVM_DISABLED_ALGORITHM_PROPERTIES = {
		"http.auth.digest.disabledAlgorithms",
		"jdk.certpath.disabledAlgorithms", "jdk.jar.disabledAlgorithms",
		"jdk.security.legacyAlgorithms", "jdk.tls.disabledAlgorithms"
	};

	private Map<String, Integer> _disabledMinKeySizes = Collections.emptyMap();

}
