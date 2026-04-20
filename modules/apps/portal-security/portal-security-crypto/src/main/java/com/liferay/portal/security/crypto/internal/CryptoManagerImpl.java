/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.crypto.internal;

import com.liferay.portal.kernel.security.fips.FIPSAuditLog;
import com.liferay.portal.kernel.security.fips.FIPSModeUtil;
import com.liferay.portal.security.crypto.manager.CryptoManager;
import com.liferay.portal.security.crypto.policy.manager.CryptoPolicyManager;
import com.liferay.portal.security.crypto.policy.manager.CryptoUseCase;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Caio Farias
 */
@Component(service = CryptoManager.class)
public class CryptoManagerImpl implements CryptoManager {

	@Override
	public SecretKey generateKey(
		CryptoUseCase cryptoUseCase, String algorithm, int keySize,
		String feature) {

		_assertAllowed(
			_OPERATION_GENERATE_KEY, cryptoUseCase, algorithm, keySize,
			feature);

		try {
			KeyGenerator keyGenerator = _getKeyGenerator(algorithm);

			keyGenerator.init(keySize);

			return keyGenerator.generateKey();
		}
		catch (GeneralSecurityException generalSecurityException) {
			throw _wrap(
				_OPERATION_GENERATE_KEY, algorithm, feature,
				generalSecurityException);
		}
	}

	@Override
	public KeyPair generateKeyPair(
		CryptoUseCase cryptoUseCase, String algorithm, int keySize,
		String feature) {

		_assertAllowed(
			_OPERATION_GENERATE_KEY_PAIR, cryptoUseCase, algorithm, keySize,
			feature);

		try {
			KeyPairGenerator keyPairGenerator = _getKeyPairGenerator(algorithm);

			keyPairGenerator.initialize(keySize);

			return keyPairGenerator.generateKeyPair();
		}
		catch (GeneralSecurityException generalSecurityException) {
			throw _wrap(
				_OPERATION_GENERATE_KEY_PAIR, algorithm, feature,
				generalSecurityException);
		}
	}

	private void _assertAllowed(
		String operation, CryptoUseCase cryptoUseCase, String algorithm,
		int keySize, String feature) {

		if (_cryptoPolicyManager.isKeySizeAllowed(
				cryptoUseCase, algorithm, keySize)) {

			return;
		}

		Map<String, Object> details = new LinkedHashMap<>();

		details.put("algorithm", algorithm);
		details.put("keySize", keySize);
		details.put("useCase", cryptoUseCase);
		details.put(
			"reason", "matched a JVM disabled-algorithms denylist entry");

		FIPSAuditLog.logCritical(operation, feature, details);

		throw new SecurityException(
			"FIPS " + operation + " refused for feature \"" + feature +
				"\": algorithm \"" + algorithm + "\" with keySize " + keySize +
					" is disabled");
	}

	private KeyGenerator _getKeyGenerator(String algorithm)
		throws GeneralSecurityException {

		if (FIPSModeUtil.isFIPSModeEnabled()) {
			return KeyGenerator.getInstance(
				algorithm, FIPSModeUtil.getFIPSProviderName());
		}

		return KeyGenerator.getInstance(algorithm);
	}

	private KeyPairGenerator _getKeyPairGenerator(String algorithm)
		throws GeneralSecurityException {

		if (FIPSModeUtil.isFIPSModeEnabled()) {
			return KeyPairGenerator.getInstance(
				algorithm, FIPSModeUtil.getFIPSProviderName());
		}

		return KeyPairGenerator.getInstance(algorithm);
	}

	private SecurityException _wrap(
		String operation, String algorithm, String feature,
		GeneralSecurityException generalSecurityException) {

		if (FIPSModeUtil.isFIPSModeEnabled()) {
			Map<String, Object> details = new LinkedHashMap<>();

			details.put("algorithm", algorithm);
			details.put(
				"exception", generalSecurityException.getClass().getName());
			details.put("message", generalSecurityException.getMessage());

			FIPSAuditLog.logCritical(operation, feature, details);
		}

		return new SecurityException(
			"Unable to " + operation + " for feature \"" + feature +
				"\" using algorithm \"" + algorithm + "\": " +
					generalSecurityException.getMessage(),
			generalSecurityException);
	}

	private static final String _OPERATION_GENERATE_KEY = "keygen.generateKey";

	private static final String _OPERATION_GENERATE_KEY_PAIR =
		"keygen.generateKeyPair";

	@Reference
	private CryptoPolicyManager _cryptoPolicyManager;

}
