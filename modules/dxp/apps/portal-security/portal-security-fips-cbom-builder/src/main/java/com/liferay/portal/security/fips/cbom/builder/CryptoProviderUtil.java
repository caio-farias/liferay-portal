/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.UnsafeFunction;
import com.liferay.portal.kernel.util.HashMapBuilder;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Provider;

import java.util.Map;

/**
 * @author Manuele Castro
 */
public class CryptoProviderUtil {

	public static String getProviderName(String algorithm, String jcaClassName)
		throws GeneralSecurityException {

		Provider provider = _getProvider(algorithm, jcaClassName);

		return provider.getName();
	}

	private static Provider _getProvider(String algorithm, String jcaClassName)
		throws GeneralSecurityException {

		UnsafeFunction<String, Provider, GeneralSecurityException>
			unsafeFunction = _providerUnsafeFunctions.get(jcaClassName);

		if (unsafeFunction == null) {
			throw new IllegalArgumentException(
				"Unsupported JCA class " + jcaClassName);
		}

		return unsafeFunction.apply(algorithm);
	}

	private static final Map
		<String, UnsafeFunction<String, Provider, GeneralSecurityException>>
			_providerUnsafeFunctions =
				HashMapBuilder.
					<String,
					 UnsafeFunction<String, Provider, GeneralSecurityException>>
						put(
							"MessageDigest",
							algorithm -> {
								MessageDigest messageDigest =
									MessageDigest.getInstance(algorithm);

								return messageDigest.getProvider();
							}
					).build();

}