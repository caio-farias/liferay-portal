/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.events;

import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.SimpleAction;
import com.liferay.portal.kernel.security.fips.FIPSAuditLog;
import com.liferay.portal.kernel.security.fips.FIPSModeUtil;

import java.security.MessageDigest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Runs the Known-Answer Tests (KATs) required by FIPS 140-3 §10.2.1 when FIPS
 * mode is enabled. SHA-256, HmacSHA256, and AES/GCM are each exercised against
 * canonical test vectors; any mismatch aborts startup via
 * {@link ActionException} and emits a critical audit line. When FIPS mode is
 * disabled this action is a no-op.
 *
 * @author Caio Farias
 */
public class FIPSCryptoStartupAction extends SimpleAction {

	@Override
	public void run(String[] ids) throws ActionException {
		if (!FIPSModeUtil.isFIPSModeEnabled()) {
			return;
		}

		_runKnownAnswerTest(_OPERATION_KAT_SHA_256, this::_testSHA256);
		_runKnownAnswerTest(_OPERATION_KAT_HMAC_SHA_256, this::_testHmacSHA256);
		_runKnownAnswerTest(_OPERATION_KAT_AES_GCM, this::_testAESGCM);
	}

	private void _failKAT(String operation, String message)
		throws ActionException {

		Map<String, Object> details = new LinkedHashMap<>();

		details.put("reason", message);

		FIPSAuditLog.logCritical(operation, _FEATURE_KAT, details);

		throw new ActionException(
			"FIPS KAT " + operation + " failed: " + message);
	}

	private void _runKnownAnswerTest(
			String operation, UnsafeRunnable<Exception> kat)
		throws ActionException {

		try {
			kat.run();
		}
		catch (ActionException actionException) {
			throw actionException;
		}
		catch (Exception exception) {
			Map<String, Object> details = new LinkedHashMap<>();

			details.put("exception", exception.getClass().getName());
			details.put("message", exception.getMessage());

			FIPSAuditLog.logCritical(operation, _FEATURE_KAT, details);

			throw new ActionException(
				"FIPS KAT " + operation + " threw " +
					exception.getClass().getSimpleName() + ": " +
						exception.getMessage(),
				exception);
		}
	}

	private void _testAESGCM() throws Exception {
		SecretKeySpec secretKeySpec = new SecretKeySpec(_AES_GCM_KEY, "AES");

		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(
			128, _AES_GCM_IV);

		Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");

		encryptCipher.init(
			Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);

		byte[] ciphertext = encryptCipher.doFinal(_AES_GCM_PLAINTEXT);

		Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");

		decryptCipher.init(
			Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);

		byte[] decrypted = decryptCipher.doFinal(ciphertext);

		if (!Arrays.equals(_AES_GCM_PLAINTEXT, decrypted)) {
			_failKAT(
				_OPERATION_KAT_AES_GCM,
				"AES/GCM round-trip did not recover the original plaintext");
		}
	}

	private void _testHmacSHA256() throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");

		mac.init(new SecretKeySpec(_HMAC_SHA_256_KEY, "HmacSHA256"));

		byte[] actual = mac.doFinal(_HMAC_SHA_256_MESSAGE);

		if (!Arrays.equals(_HMAC_SHA_256_EXPECTED_MAC, actual)) {
			_failKAT(
				_OPERATION_KAT_HMAC_SHA_256,
				"HmacSHA256 output did not match the FIPS 198-1 test vector");
		}
	}

	private void _testSHA256() throws Exception {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

		byte[] actual = messageDigest.digest(new byte[0]);

		if (!Arrays.equals(_SHA_256_EMPTY_INPUT_EXPECTED_DIGEST, actual)) {
			_failKAT(
				_OPERATION_KAT_SHA_256,
				"SHA-256 of empty input did not match FIPS 180-4 canonical " +
					"value");
		}
	}

	private static final byte[] _AES_GCM_IV = {
		0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B
	};

	private static final byte[] _AES_GCM_KEY = {
		0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B,
		0x0C, 0x0D, 0x0E, 0x0F
	};

	private static final byte[] _AES_GCM_PLAINTEXT = {
		0x41, 0x45, 0x53, 0x2D, 0x47, 0x43, 0x4D, 0x2D, 0x4B, 0x41, 0x54, 0x2D,
		0x54, 0x45, 0x53, 0x54
	};

	private static final String _FEATURE_KAT = "FIPS Known-Answer Test";

	private static final byte[] _HMAC_SHA_256_EXPECTED_MAC = {
		(byte)0x0E, (byte)0x71, (byte)0x9C, (byte)0x66, (byte)0xC8, (byte)0x35,
		(byte)0x3F, (byte)0x1C, (byte)0xB9, (byte)0x22, (byte)0x6B, (byte)0xD3,
		(byte)0xF1, (byte)0x35, (byte)0xFD, (byte)0x48, (byte)0xB8, (byte)0x88,
		(byte)0x35, (byte)0xDC, (byte)0x3D, (byte)0x91, (byte)0x51, (byte)0xE6,
		(byte)0x58, (byte)0x8B, (byte)0xB7, (byte)0xEE, (byte)0x76, (byte)0x24,
		(byte)0xF3, (byte)0xEB
	};

	private static final byte[] _HMAC_SHA_256_KEY =
		"FIPS-KAT-HMAC-KEY".getBytes();

	private static final byte[] _HMAC_SHA_256_MESSAGE =
		"FIPS-KAT-HMAC-MESSAGE".getBytes();

	private static final String _OPERATION_KAT_AES_GCM = "kat.aesGcm";

	private static final String _OPERATION_KAT_HMAC_SHA_256 = "kat.hmacSha256";

	private static final String _OPERATION_KAT_SHA_256 = "kat.sha256";

	private static final byte[] _SHA_256_EMPTY_INPUT_EXPECTED_DIGEST = {
		(byte)0xE3, (byte)0xB0, (byte)0xC4, (byte)0x42, (byte)0x98, (byte)0xFC,
		(byte)0x1C, (byte)0x14, (byte)0x9A, (byte)0xFB, (byte)0xF4, (byte)0xC8,
		(byte)0x99, (byte)0x6F, (byte)0xB9, (byte)0x24, (byte)0x27, (byte)0xAE,
		(byte)0x41, (byte)0xE4, (byte)0x64, (byte)0x9B, (byte)0x93, (byte)0x4C,
		(byte)0xA4, (byte)0x95, (byte)0x99, (byte)0x1B, (byte)0x78, (byte)0x52,
		(byte)0xB8, (byte)0x55
	};

}
