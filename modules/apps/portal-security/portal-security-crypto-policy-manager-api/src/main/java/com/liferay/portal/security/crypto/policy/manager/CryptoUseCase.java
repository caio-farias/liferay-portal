/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.manager;

import java.util.Set;

/**
 * Classifies the standard {@code java.security.Provider.Service} types into
 * the FIPS 140-3 use-case buckets consulted by {@link CryptoPolicyManager}
 * and downstream crypto services.
 *
 * @author Caio Farias
 */
public enum CryptoUseCase {

	DATA_PROTECTION(Set.of("Cipher", "Mac", "MessageDigest", "Signature")),
	INFRASTRUCTURE(
		Set.of(
			"CertPathBuilder", "CertPathValidator", "CertStore",
			"CertificateFactory", "KeyManagerFactory", "SSLContext",
			"TrustManagerFactory")),
	KEY_MANAGEMENT(
		Set.of(
			"AlgorithmParameterGenerator", "AlgorithmParameters",
			"KeyAgreement", "KeyFactory", "KeyGenerator", "KeyPairGenerator",
			"SecretKeyFactory")),
	RANDOMNESS(Set.of("SecureRandom")),
	STORAGE(Set.of("KeyStore")),
	IGNORED(Set.of());

	public Set<String> getServiceTypes() {
		return _serviceTypes;
	}

	private final Set<String> _serviceTypes;

	private CryptoUseCase(Set<String> serviceTypes) {
		_serviceTypes = serviceTypes;
	}

}
