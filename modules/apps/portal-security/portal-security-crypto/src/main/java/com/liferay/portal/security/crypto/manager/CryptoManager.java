/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.manager;

import com.liferay.portal.security.crypto.policy.manager.CryptoUseCase;

import java.security.KeyPair;

import javax.crypto.SecretKey;

/**
 * Single entry point for cryptographic key generation in Liferay. When FIPS
 * mode is enabled, generation is routed through the configured FIPS provider
 * so keys inherit entropy from its approved DRBG, and
 * {@link com.liferay.portal.security.crypto.policy.manager.CryptoPolicyManager}
 * is consulted to reject any algorithm or key size on the JVM's
 * disabled-algorithms denylist. Violations emit a critical
 * {@code FIPSAuditLog} line and throw {@link SecurityException}. When FIPS
 * mode is disabled the default JCE provider chain is used and no policy is
 * applied.
 *
 * @author Caio Farias
 */
public interface CryptoManager {

	public SecretKey generateKey(
		CryptoUseCase cryptoUseCase, String algorithm, int keySize,
		String feature);

	public KeyPair generateKeyPair(
		CryptoUseCase cryptoUseCase, String algorithm, int keySize,
		String feature);

}
