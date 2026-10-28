/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.portal.kernel.util.ListUtil;

import java.util.List;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

/**
 * @author Caio Farias
 */
public class CryptoUse {

	public CryptoUse(
		String algorithm, List<String> classNames, List<String> configuredBy,
		List<CryptoFunction> cryptoFunctions, String featureName,
		List<String> jcaClassNames, Primitive primitive) {

		_algorithm = algorithm;
		_classNames = classNames;
		_configuredBy = configuredBy;
		_cryptoFunctions = cryptoFunctions;
		_featureName = featureName;
		_jcaClassNames = jcaClassNames;
		_primitive = primitive;
	}

	public String getAlgorithm() {
		return _algorithm;
	}

	public List<String> getClassNames() {
		return _classNames;
	}

	public List<String> getConfiguredBy() {
		return _configuredBy;
	}

	public List<CryptoFunction> getCryptoFunctions() {
		return _cryptoFunctions;
	}

	public String getFeatureName() {
		return _featureName;
	}

	public List<String> getJCAClassNames() {
		return _jcaClassNames;
	}

	public Primitive getPrimitive() {
		return _primitive;
	}

	public boolean isConfigurable() {
		return ListUtil.isNotEmpty(_configuredBy);
	}

	private final String _algorithm;
	private final List<String> _classNames;
	private final List<String> _configuredBy;
	private final List<CryptoFunction> _cryptoFunctions;
	private final String _featureName;
	private final List<String> _jcaClassNames;
	private final Primitive _primitive;

}