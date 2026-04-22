/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.policy.manager;

/**
 * Answers FIPS 140-3 policy questions about whether an algorithm, or an
 * algorithm paired with a key size, may be used for a given
 * {@link CryptoUseCase}. Backed by the union of the JVM's standard
 * disabled-algorithms security properties so operators configure policy in
 * one place ({@code java.security}) and every consumer — keygen, UI dropdowns,
 * admin tools — sees the same answer.
 *
 * <p>
 * When FIPS mode is disabled, both {@code isAlgorithmAllowed} and
 * {@code isKeySizeAllowed} return {@code true} unconditionally: no FIPS
 * policy is in effect.
 * </p>
 *
 * @author Caio Farias
 */
public interface CryptoPolicyManager {

	public boolean isAlgorithmAllowed(
		CryptoUseCase cryptoUseCase, String algorithm);

	public boolean isApprovedMode();

	public boolean isKeySizeAllowed(
		CryptoUseCase cryptoUseCase, String algorithm, int keySize);

}
