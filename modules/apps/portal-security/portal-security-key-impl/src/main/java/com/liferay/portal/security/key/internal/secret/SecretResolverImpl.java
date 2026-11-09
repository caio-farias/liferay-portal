/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.key.internal.secret;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.cache.PortalCacheHelperUtil;
import com.liferay.portal.kernel.cache.PortalCacheManagerNames;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.security.fips.FIPSAuditEvent;
import com.liferay.portal.kernel.security.fips.FIPSAuditUtil;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.settings.CompanyServiceSettingsLocator;
import com.liferay.portal.kernel.settings.GroupServiceSettingsLocator;
import com.liferay.portal.kernel.settings.Settings;
import com.liferay.portal.kernel.settings.SettingsLocator;
import com.liferay.portal.kernel.settings.SettingsLocatorHelper;
import com.liferay.portal.kernel.settings.SystemSettingsLocator;
import com.liferay.portal.kernel.settings.definition.ConfigurationPidMapping;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.key.KeyReference;
import com.liferay.portal.security.key.KeyReferenceUtil;
import com.liferay.portal.security.key.secret.Secret;
import com.liferay.portal.security.key.secret.SecretContext;
import com.liferay.portal.security.key.secret.SecretManager;
import com.liferay.portal.security.key.secret.SecretResolver;
import com.liferay.portal.security.key.secret.exception.SecretException;
import com.liferay.portal.security.key.spi.profile.KeyManagerProfile;
import com.liferay.portal.security.key.spi.profile.KeyManagerProfileRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Pedro Victor Silvestre
 */
@Component(service = SecretResolver.class)
public class SecretResolverImpl implements SecretResolver {

	public static final String PORTAL_CACHE_NAME =
		SecretResolverImpl.class.getName();

	public static String getKey(long companyId, String keyReferenceString) {
		return StringBundler.concat(
			companyId, StringPool.POUND, keyReferenceString);
	}

	@Override
	public String resolve(
		long companyId, SecretContext secretContext, String value) {

		if (!KeyReferenceUtil.isKeyReference(value)) {
			return value;
		}

		try {
			KeyReference keyReference = _getKeyReference(value);

			long scopeCompanyId = _getScopeCompanyId(
				companyId, keyReference.getIdentifier(), "resolve",
				_getScopeCompanyIds(secretContext));

			String key = getKey(scopeCompanyId, value);

			String resolvedValue = _portalCache.get(key);

			if (resolvedValue != null) {
				return resolvedValue;
			}

			try (Secret secret = _secretManager.getSecret(
					scopeCompanyId, keyReference)) {

				resolvedValue = new String(secret.getChars());
			}

			_portalCache.put(key, resolvedValue, 600);

			return resolvedValue;
		}
		catch (PortalException portalException) {
			return ReflectionUtil.throwException(portalException);
		}
	}

	@Override
	public String resolve(long companyId, String value) {
		if (!KeyReferenceUtil.isKeyReference(value)) {
			return value;
		}

		try {
			KeyReference keyReference = _getKeyReference(value);

			if (companyId != CompanyConstants.SYSTEM) {
				KeyManagerProfile keyManagerProfile =
					_keyManagerProfileRegistry.getActiveKeyManagerProfile();

				if ((keyManagerProfile != null) &&
					Objects.equals(
						keyReference.getProviderId(),
						keyManagerProfile.getSystemSecretProviderId())) {

					companyId = CompanyConstants.SYSTEM;
				}
			}

			String key = getKey(companyId, value);

			String resolvedValue = _portalCache.get(key);

			if (resolvedValue != null) {
				return resolvedValue;
			}

			try (Secret secret = _secretManager.getSecret(
					companyId, keyReference)) {

				resolvedValue = new String(secret.getChars());
			}

			_portalCache.put(key, resolvedValue, 600);

			return resolvedValue;
		}
		catch (SecretException secretException) {
			return ReflectionUtil.throwException(secretException);
		}
	}

	@Override
	public String resolve(long companyId, String identifier, String value) {
		KeyReference keyReference = KeyReferenceUtil.parseKeyReference(value);

		if ((keyReference == null) ||
			!Objects.equals(identifier, keyReference.getIdentifier())) {

			return value;
		}

		return resolve(companyId, value);
	}

	@Override
	public String store(
		long companyId, SecretContext secretContext, String value) {

		if (!PropsValues.FIPS_ENABLED || Validator.isNull(value)) {
			return value;
		}

		try {
			SequencedMap<String, Long> scopeCompanyIds = _getScopeCompanyIds(
				secretContext);

			if (KeyReferenceUtil.isKeyReference(value)) {
				KeyReference keyReference = _getKeyReference(value);

				_getScopeCompanyId(
					companyId, keyReference.getIdentifier(), "store",
					scopeCompanyIds);

				return value;
			}

			Map.Entry<String, Long> entry = scopeCompanyIds.firstEntry();

			String identifier = entry.getKey();

			long scopeCompanyId = _getScopeCompanyId(
				companyId, identifier, "store", scopeCompanyIds);

			try (Secret secret = new Secret(
					new KeyReference(
						identifier, StringPool.STAR, KeyReference.Type.SECRET),
					value)) {

				return _putSecret(scopeCompanyId, secret);
			}
		}
		catch (PortalException portalException) {
			return ReflectionUtil.throwException(portalException);
		}
	}

	@Override
	public String store(long companyId, String identifier, String value) {
		if (!PropsValues.FIPS_ENABLED || Validator.isNull(value)) {
			return value;
		}

		if (!identifier.startsWith("config/") &&
			!identifier.startsWith(_IDENTIFIER_PREFIX_PREFERENCE)) {

			throw new IllegalArgumentException(
				StringBundler.concat(
					"Unable to store \"", identifier,
					"\" because its namespace is not supported"));
		}

		try {
			if (KeyReferenceUtil.isKeyReference(value)) {
				KeyReference keyReference = KeyReferenceUtil.parseKeyReference(
					value);

				if (keyReference == null) {
					throw new SecretException(
						"Unable to parse the key reference");
				}

				String keyReferenceIdentifier = keyReference.getIdentifier();

				if (Objects.equals(identifier, keyReferenceIdentifier) ||
					(identifier.startsWith(_IDENTIFIER_PREFIX_PREFERENCE) &&
					 keyReferenceIdentifier.startsWith(
						 _IDENTIFIER_PREFIX_PREFERENCE) &&
					 Objects.equals(
						 StringUtil.extractLast(identifier, CharPool.SLASH),
						 StringUtil.extractLast(
							 keyReferenceIdentifier, CharPool.SLASH)))) {

					return value;
				}

				throw new SecretException(
					StringBundler.concat(
						"Unable to store \"", identifier,
						"\" because it references \"", keyReferenceIdentifier,
						"\""));
			}

			try (Secret secret = new Secret(
					new KeyReference(
						identifier, StringPool.STAR, KeyReference.Type.SECRET),
					value)) {

				return _putSecret(companyId, secret);
			}
		}
		catch (SecretException secretException) {
			return ReflectionUtil.throwException(secretException);
		}
	}

	@Activate
	protected void activate() {
		_portalCache = PortalCacheHelperUtil.getPortalCache(
			PortalCacheManagerNames.SINGLE_VM, PORTAL_CACHE_NAME);
	}

	@Deactivate
	protected void deactivate() {
		PortalCacheHelperUtil.removePortalCache(
			PortalCacheManagerNames.SINGLE_VM, PORTAL_CACHE_NAME);
	}

	private void _audit(long companyId, String identifier, String operation) {
		FIPSAuditEvent fipsAuditEvent = new FIPSAuditEvent(
			"secret-reference-refused", FIPSAuditEvent.Severity.WARNING);

		fipsAuditEvent.put("company-id", companyId);
		fipsAuditEvent.put("identifier", identifier);
		fipsAuditEvent.put("operation", operation);

		FIPSAuditUtil.write(fipsAuditEvent);
	}

	private Long _fetchScopeCompanyId(
		long companyId, String identifier, Map<String, Long> scopeCompanyIds) {

		Long scopeCompanyId = scopeCompanyIds.get(identifier);

		if ((scopeCompanyId != null) &&
			((scopeCompanyId == CompanyConstants.SYSTEM) ||
			 (scopeCompanyId == companyId))) {

			return scopeCompanyId;
		}

		return null;
	}

	private long _getCounterpartGroupId(Group group) {
		if (group.isStagingGroup()) {
			return group.getLiveGroupId();
		}

		Group stagingGroup = group.getStagingGroup();

		if (stagingGroup == null) {
			return 0;
		}

		return stagingGroup.getGroupId();
	}

	private KeyReference _getKeyReference(String value) throws SecretException {
		KeyReference keyReference = KeyReferenceUtil.toKeyReference(value);

		if (keyReference.getType() != KeyReference.Type.SECRET) {
			throw new SecretException(
				"Crypto key references are not supported by the secret " +
					"resolver");
		}

		return keyReference;
	}

	private String _getPreferenceIdentifier(
		String key, ExtendedObjectClassDefinition.Scope scope,
		Object scopePrimaryKey, String settingsId) {

		return StringBundler.concat(
			_IDENTIFIER_PREFIX_PREFERENCE, settingsId, StringPool.SLASH,
			scope.getValue(), StringPool.SLASH, scopePrimaryKey,
			StringPool.SLASH, key);
	}

	private long _getScopeCompanyId(
			long companyId, String identifier, String operation,
			Map<String, Long> scopeCompanyIds)
		throws SecretException {

		Long scopeCompanyId = _fetchScopeCompanyId(
			companyId, identifier, scopeCompanyIds);

		if (scopeCompanyId != null) {
			return scopeCompanyId;
		}

		_audit(companyId, identifier, operation);

		throw new SecretException(
			StringBundler.concat(
				"Unable to ", operation, " the key reference \"", identifier,
				"\" in this secret context"));
	}

	private SequencedMap<String, Long> _getScopeCompanyIds(
			SecretContext secretContext)
		throws PortalException {

		SequencedMap<String, Long> scopeCompanyIds = new LinkedHashMap<>();

		SecretContext.Type type = secretContext.getType();

		if (type == SecretContext.Type.COMPANY) {
			_putPortalPreferenceScope(
				secretContext.getCompanyId(), secretContext.getKey(),
				scopeCompanyIds);
		}
		else if (type == SecretContext.Type.CONFIGURATION) {
			Map<String, Object> properties = secretContext.getProperties();

			String pid = GetterUtil.getString(
				properties.get(Constants.SERVICE_PID));

			scopeCompanyIds.put(
				SecretIdentifierUtil.getConfigurationIdentifier(
					secretContext.getKey(), pid, properties::get),
				SecretIdentifierUtil.getConfigurationCompanyId(
					properties::get));
		}
		else {
			_putSettingsScopes(
				secretContext.getKey(), scopeCompanyIds,
				secretContext.getSettingsLocator());
		}

		return scopeCompanyIds;
	}

	private void _putCompanyScopes(
		long companyId, String configurationPid, String key,
		Map<String, Long> scopeCompanyIds, String settingsId) {

		_putScope(
			companyId, configurationPid, key,
			ExtendedObjectClassDefinition.Scope.COMPANY, scopeCompanyIds,
			companyId, settingsId);
		_putPortalPreferenceScope(companyId, key, scopeCompanyIds);
		_putSystemScope(configurationPid, key, scopeCompanyIds);
	}

	private void _putGroupScopes(
		String configurationPid, Group group, String key,
		Map<String, Long> scopeCompanyIds, String settingsId) {

		_putScope(
			group.getCompanyId(), configurationPid, key,
			ExtendedObjectClassDefinition.Scope.GROUP, scopeCompanyIds,
			group.getGroupId(), settingsId);

		long counterpartGroupId = _getCounterpartGroupId(group);

		if (counterpartGroupId > 0) {
			_putScope(
				group.getCompanyId(), configurationPid, key,
				ExtendedObjectClassDefinition.Scope.GROUP, scopeCompanyIds,
				counterpartGroupId, settingsId);
		}
	}

	private void _putPortalPreferenceScope(
		long companyId, String key, Map<String, Long> scopeCompanyIds) {

		scopeCompanyIds.put(
			StringBundler.concat(
				"preference/portal/", companyId, StringPool.SLASH, key),
			companyId);
	}

	private void _putScope(
		long companyId, String configurationPid, String key,
		ExtendedObjectClassDefinition.Scope scope,
		Map<String, Long> scopeCompanyIds, long scopePrimaryKey,
		String settingsId) {

		scopeCompanyIds.put(
			_getPreferenceIdentifier(key, scope, scopePrimaryKey, settingsId),
			companyId);
		scopeCompanyIds.put(
			SecretIdentifierUtil.getConfigurationIdentifier(
				key, configurationPid, scope, scopePrimaryKey),
			companyId);
	}

	private String _putSecret(long scopeCompanyId, Secret secret)
		throws SecretException {

		KeyReference keyReference = _secretManager.putSecret(
			scopeCompanyId, secret);

		return KeyReferenceUtil.toKeyReferenceString(keyReference);
	}

	private void _putSettingsScopes(
			String key, Map<String, Long> scopeCompanyIds,
			SettingsLocator settingsLocator)
		throws PortalException {

		if (settingsLocator instanceof
				CompanyServiceSettingsLocator companyServiceSettingsLocator) {

			_putCompanyScopes(
				companyServiceSettingsLocator.getCompanyId(),
				companyServiceSettingsLocator.getConfigurationPid(), key,
				scopeCompanyIds, companyServiceSettingsLocator.getSettingsId());
		}
		else if (settingsLocator instanceof
					GroupServiceSettingsLocator groupServiceSettingsLocator) {

			Group group = _groupLocalService.getGroup(
				groupServiceSettingsLocator.getGroupId());

			_putGroupScopes(
				groupServiceSettingsLocator.getConfigurationPid(), group, key,
				scopeCompanyIds, groupServiceSettingsLocator.getSettingsId());

			_putCompanyScopes(
				group.getCompanyId(),
				groupServiceSettingsLocator.getConfigurationPid(), key,
				scopeCompanyIds, groupServiceSettingsLocator.getSettingsId());
		}
		else if (settingsLocator instanceof SystemSettingsLocator) {
			_putSystemScope(
				settingsLocator.getSettingsId(), key, scopeCompanyIds);
		}
		else {
			throw new IllegalArgumentException(
				"Unsupported settings locator " + settingsLocator.getClass());
		}
	}

	private void _putSystemScope(
		String configurationPid, String key,
		Map<String, Long> scopeCompanyIds) {

		scopeCompanyIds.put(
			SecretIdentifierUtil.getConfigurationIdentifier(
				key, _toOCDPid(configurationPid),
				ExtendedObjectClassDefinition.Scope.SYSTEM,
				CompanyConstants.SYSTEM),
			CompanyConstants.SYSTEM);
		scopeCompanyIds.put("property/" + key, CompanyConstants.SYSTEM);
	}

	private String _toOCDPid(String configurationPid) {
		ConfigurationPidMapping configurationPidMapping =
			_settingsLocatorHelper.getConfigurationPidMapping(configurationPid);

		if (configurationPidMapping == null) {
			return configurationPid;
		}

		Class<?> clazz = configurationPidMapping.getConfigurationBeanClass();

		if (clazz.getAnnotation(Settings.Config.class) != null) {
			return configurationPid;
		}

		Meta.OCD ocd = clazz.getAnnotation(Meta.OCD.class);

		if (ocd == null) {
			return configurationPid;
		}

		return ocd.id();
	}

	private static final String _IDENTIFIER_PREFIX_PREFERENCE = "preference/";

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private KeyManagerProfileRegistry _keyManagerProfileRegistry;

	private PortalCache<String, String> _portalCache;

	@Reference
	private SecretManager _secretManager;

	@Reference
	private SettingsLocatorHelper _settingsLocatorHelper;

}