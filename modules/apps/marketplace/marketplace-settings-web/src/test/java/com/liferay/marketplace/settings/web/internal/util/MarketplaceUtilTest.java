/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.marketplace.settings.web.internal.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.module.service.Snapshot;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.PrefsProps;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.security.key.KeyReference;
import com.liferay.portal.security.key.KeyReferenceUtil;
import com.liferay.portal.security.key.secret.Secret;
import com.liferay.portal.security.key.secret.SecretManager;
import com.liferay.portal.security.key.secret.exception.SecretException;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Caio Farias
 */
public class MarketplaceUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		_prefsProps = ReflectionTestUtil.getAndSetFieldValue(
			PrefsPropsUtil.class, "_prefsProps", _mockPrefsProps);
		_secretManagerSnapshot = ReflectionTestUtil.getAndSetFieldValue(
			MarketplaceUtil.class, "_secretManagerSnapshot",
			new Snapshot<SecretManager>(
				MarketplaceUtil.class, SecretManager.class) {

				@Override
				public SecretManager get() {
					return _secretManager;
				}

			});
	}

	@After
	public void tearDown() {
		ReflectionTestUtil.setFieldValue(
			MarketplaceUtil.class, "_secretManagerSnapshot",
			_secretManagerSnapshot);
		ReflectionTestUtil.setFieldValue(
			PrefsPropsUtil.class, "_prefsProps", _prefsProps);
	}

	@Test
	public void testDeleteTokens() throws Exception {
		long companyId = RandomTestUtil.randomLong();

		KeyReference accessTokenKeyReference = new KeyReference(
			_getIdentifier(
				companyId, "marketplaceAccessToken",
				RandomTestUtil.randomString()),
			RandomTestUtil.randomString(), KeyReference.Type.SECRET);

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceAccessToken")
		).thenReturn(
			KeyReferenceUtil.toKeyReferenceString(accessTokenKeyReference)
		);

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceRefreshToken")
		).thenReturn(
			RandomTestUtil.randomString()
		);

		MarketplaceUtil.deleteTokens(companyId);

		Mockito.verify(
			_secretManager
		).deleteSecret(
			companyId, accessTokenKeyReference
		);

		Mockito.verifyNoMoreInteractions(_secretManager);

		Mockito.clearInvocations(_secretManager);

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceAccessToken")
		).thenReturn(
			KeyReferenceUtil.toKeyReferenceString(
				new KeyReference(
					_getIdentifier(
						companyId + 1, "marketplaceAccessToken",
						RandomTestUtil.randomString()),
					RandomTestUtil.randomString(), KeyReference.Type.SECRET))
		);

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceRefreshToken")
		).thenReturn(
			KeyReferenceUtil.toKeyReferenceString(
				new KeyReference(
					_getIdentifier(
						companyId, "marketplaceAccessToken",
						RandomTestUtil.randomString()),
					RandomTestUtil.randomString(), KeyReference.Type.SECRET))
		);

		MarketplaceUtil.deleteTokens(companyId);

		Mockito.verifyNoInteractions(_secretManager);

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceAccessToken")
		).thenReturn(
			KeyReferenceUtil.toKeyReferenceString(accessTokenKeyReference)
		);

		KeyReference refreshTokenKeyReference = new KeyReference(
			_getIdentifier(
				companyId, "marketplaceRefreshToken",
				RandomTestUtil.randomString()),
			RandomTestUtil.randomString(), KeyReference.Type.SECRET);

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceRefreshToken")
		).thenReturn(
			KeyReferenceUtil.toKeyReferenceString(refreshTokenKeyReference)
		);

		Mockito.doThrow(
			new SecretException()
		).when(
			_secretManager
		).deleteSecret(
			companyId, accessTokenKeyReference
		);

		try (LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				MarketplaceUtil.class.getName(), LoggerTestUtil.ERROR)) {

			MarketplaceUtil.deleteTokens(companyId);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(
				"Unable to delete the stored value of " +
					"\"marketplaceAccessToken\" for company " + companyId,
				logEntry.getMessage());
		}

		Mockito.verify(
			_secretManager
		).deleteSecret(
			companyId, refreshTokenKeyReference
		);
	}

	@Test
	public void testGetToken() throws Exception {
		long companyId = RandomTestUtil.randomLong();
		String token = RandomTestUtil.randomString();

		KeyReference keyReference = new KeyReference(
			_getIdentifier(
				companyId, "marketplaceAccessToken",
				RandomTestUtil.randomString()),
			RandomTestUtil.randomString(), KeyReference.Type.SECRET);

		Mockito.when(
			_secretManager.getSecret(companyId, keyReference)
		).thenReturn(
			new Secret(keyReference, token)
		);

		_testGetToken(
			companyId, token,
			KeyReferenceUtil.toKeyReferenceString(keyReference));

		_testGetToken(companyId, token, token);

		String keyReferenceString = KeyReferenceUtil.toKeyReferenceString(
			new KeyReference(
				_getIdentifier(
					companyId, "marketplaceRefreshToken",
					RandomTestUtil.randomString()),
				RandomTestUtil.randomString(), KeyReference.Type.SECRET));

		_testGetToken(companyId, keyReferenceString, keyReferenceString);

		keyReferenceString = KeyReferenceUtil.toKeyReferenceString(
			new KeyReference(
				_getIdentifier(
					companyId + 1, "marketplaceAccessToken",
					RandomTestUtil.randomString()),
				RandomTestUtil.randomString(), KeyReference.Type.SECRET));

		_testGetToken(companyId, keyReferenceString, keyReferenceString);
	}

	@Test
	public void testStoreToken() throws Exception {
		long companyId = RandomTestUtil.randomLong();
		String providerId = RandomTestUtil.randomString();
		String token = RandomTestUtil.randomString();
		String uuid = RandomTestUtil.randomString();

		Assert.assertEquals(
			token, _storeToken(companyId, "marketplaceRefreshToken", token));

		Mockito.when(
			_secretManager.putSecret(
				Mockito.eq(companyId), Mockito.any(Secret.class))
		).thenAnswer(
			invocationOnMock -> {
				Secret secret = invocationOnMock.getArgument(1);

				Assert.assertEquals(token, new String(secret.getChars()));

				KeyReference keyReference = secret.getKeyReference();

				Assert.assertEquals(
					StringPool.STAR, keyReference.getProviderId());

				return new KeyReference(
					keyReference.getIdentifier(), providerId,
					KeyReference.Type.SECRET);
			}
		);

		try (AutoCloseable autoCloseable =
				ReflectionTestUtil.setFieldValueWithAutoCloseable(
					PropsValues.class, "FIPS_ENABLED", true);
			MockedStatic<PortalUUIDUtil> portalUUIDUtilMockedStatic =
				Mockito.mockStatic(PortalUUIDUtil.class)) {

			portalUUIDUtilMockedStatic.when(
				PortalUUIDUtil::generate
			).thenReturn(
				uuid
			);

			Assert.assertNull(
				_storeToken(companyId, "marketplaceRefreshToken", null));
			Assert.assertEquals(
				StringPool.BLANK,
				_storeToken(
					companyId, "marketplaceRefreshToken", StringPool.BLANK));

			Mockito.verifyNoInteractions(_secretManager);

			String keyReferenceString = KeyReferenceUtil.toKeyReferenceString(
				new KeyReference(
					_getIdentifier(companyId, "marketplaceRefreshToken", uuid),
					providerId, KeyReference.Type.SECRET));

			Assert.assertEquals(
				keyReferenceString,
				_storeToken(companyId, "marketplaceRefreshToken", token));

			Mockito.when(
				_mockPrefsProps.getString(companyId, "marketplaceRefreshToken")
			).thenReturn(
				KeyReferenceUtil.toKeyReferenceString(
					new KeyReference(
						_getIdentifier(
							companyId + 1, "marketplaceRefreshToken",
							RandomTestUtil.randomString()),
						providerId, KeyReference.Type.SECRET))
			);

			Assert.assertEquals(
				keyReferenceString,
				_storeToken(companyId, "marketplaceRefreshToken", token));

			String identifier = _getIdentifier(
				companyId, "marketplaceRefreshToken",
				RandomTestUtil.randomString());

			Mockito.when(
				_mockPrefsProps.getString(companyId, "marketplaceRefreshToken")
			).thenReturn(
				KeyReferenceUtil.toKeyReferenceString(
					new KeyReference(
						identifier, providerId, KeyReference.Type.SECRET))
			);

			Assert.assertEquals(
				KeyReferenceUtil.toKeyReferenceString(
					new KeyReference(
						identifier, providerId, KeyReference.Type.SECRET)),
				_storeToken(companyId, "marketplaceRefreshToken", token));

			Mockito.doThrow(
				new SecretException()
			).when(
				_secretManager
			).putSecret(
				Mockito.eq(companyId), Mockito.any(Secret.class)
			);

			Assert.assertThrows(
				SecretException.class,
				() -> _storeToken(companyId, "marketplaceRefreshToken", token));
		}

		PortalException portalException = Assert.assertThrows(
			PortalException.class,
			() -> _storeToken(
				companyId, "marketplaceRefreshToken",
				KeyReferenceUtil.toKeyReferenceString(
					new KeyReference(
						RandomTestUtil.randomString(), StringPool.STAR,
						KeyReference.Type.SECRET))));

		Assert.assertEquals(
			"Token cannot begin with a reserved key reference prefix",
			portalException.getMessage());
	}

	private String _getIdentifier(long companyId, String name, String uuid) {
		return StringBundler.concat(
			"Marketplace/", companyId, StringPool.SLASH, name, StringPool.SLASH,
			uuid);
	}

	private String _storeToken(long companyId, String name, String token)
		throws Exception {

		return ReflectionTestUtil.invoke(
			MarketplaceUtil.class, "_storeToken",
			new Class<?>[] {long.class, String.class, String.class}, companyId,
			name, token);
	}

	private void _testGetToken(
			long companyId, String expectedToken, String storedToken)
		throws Exception {

		Mockito.when(
			_mockPrefsProps.getString(companyId, "marketplaceAccessToken")
		).thenReturn(
			storedToken
		);

		Assert.assertEquals(
			expectedToken,
			MarketplaceUtil.getToken(companyId, "marketplaceAccessToken"));
	}

	private final PrefsProps _mockPrefsProps = Mockito.mock(PrefsProps.class);
	private PrefsProps _prefsProps;
	private final SecretManager _secretManager = Mockito.mock(
		SecretManager.class);
	private Snapshot<SecretManager> _secretManagerSnapshot;

}