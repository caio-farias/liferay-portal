/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;

import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.crypto.AlgorithmProperties;
import org.cyclonedx.model.component.crypto.CryptoProperties;
import org.cyclonedx.model.component.crypto.enums.AssetType;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.evidence.Occurrence;

/**
 * @author Manuele Castro
 */
public class CryptoAssetComponentUtil {

	public static String getBOMRef(CryptoUse cryptoUse) {
		List<String> jcaClassNames = cryptoUse.getJCAClassNames();

		return StringBundler.concat(
			"crypto:", jcaClassNames.get(0), StringPool.COLON,
			_getName(cryptoUse));
	}

	public static List<Component> toComponents(
			List<CryptoUse> cryptoUses, Properties portalProperties)
		throws GeneralSecurityException {

		Map<String, List<CryptoUse>> cryptoUsesMap = new TreeMap<>();

		for (CryptoUse cryptoUse : cryptoUses) {
			String bomRef = getBOMRef(cryptoUse);

			List<CryptoUse> bomRefCryptoUses = cryptoUsesMap.computeIfAbsent(
				bomRef, key -> new ArrayList<>());

			if (!bomRefCryptoUses.isEmpty()) {
				_validate(bomRef, bomRefCryptoUses.get(0), cryptoUse);
			}

			bomRefCryptoUses.add(cryptoUse);
		}

		List<Component> components = new ArrayList<>();

		for (Map.Entry<String, List<CryptoUse>> entry :
				cryptoUsesMap.entrySet()) {

			Component component = _toComponent(
				entry.getKey(), entry.getValue(), portalProperties);

			components.add(component);
		}

		return components;
	}

	private static List<CryptoFunction> _getCryptoFunctions(
		List<CryptoUse> cryptoUses) {

		Set<CryptoFunction> cryptoFunctions = new TreeSet<>();

		for (CryptoUse cryptoUse : cryptoUses) {
			cryptoFunctions.addAll(cryptoUse.getCryptoFunctions());
		}

		return new ArrayList<>(cryptoFunctions);
	}

	private static Evidence _getEvidence(List<CryptoUse> cryptoUses) {
		Set<String> classNames = new TreeSet<>();

		for (CryptoUse cryptoUse : cryptoUses) {
			classNames.addAll(cryptoUse.getClassNames());
		}

		List<Occurrence> occurrences = TransformUtil.transform(
			classNames,
			className -> {
				Occurrence occurrence = new Occurrence();

				occurrence.setLocation(className);

				return occurrence;
			});

		Evidence evidence = new Evidence();

		evidence.setOccurrences(occurrences);

		return evidence;
	}

	private static String _getName(CryptoUse cryptoUse) {
		if (cryptoUse.isConfigurable()) {
			List<String> configuredBy = cryptoUse.getConfiguredBy();

			return configuredBy.get(0);
		}

		return cryptoUse.getAlgorithm();
	}

	private static List<Property> _getProperties(
			List<CryptoUse> cryptoUses, Properties portalProperties)
		throws GeneralSecurityException {

		List<Property> properties = new ArrayList<>();

		CryptoUse cryptoUse = cryptoUses.get(0);

		for (String propertyKey : cryptoUse.getConfiguredBy()) {
			properties.add(
				new Property("liferay:cbom:configured-by", propertyKey));
		}

		for (String propertyKey : cryptoUse.getConfiguredBy()) {
			String value = portalProperties.getProperty(
				propertyKey, StringPool.BLANK);

			properties.add(
				new Property(
					"liferay:cbom:default-value",
					propertyKey + StringPool.EQUAL + value));
		}

		Set<String> featureNames = new TreeSet<>();

		for (CryptoUse bomRefCryptoUse : cryptoUses) {
			featureNames.add(bomRefCryptoUse.getFeatureName());
		}

		for (String featureName : featureNames) {
			properties.add(new Property("liferay:cbom:feature", featureName));
		}

		List<String> jcaClassNames = cryptoUse.getJCAClassNames();

		for (String jcaClassName : jcaClassNames) {
			properties.add(
				new Property("liferay:cbom:jca-class", jcaClassName));
		}

		if (!cryptoUse.isConfigurable()) {
			String providerName = CryptoProviderUtil.getProviderName(
				cryptoUse.getAlgorithm(), jcaClassNames.get(0));

			properties.add(new Property("liferay:cbom:provider", providerName));
		}

		return properties;
	}

	private static Component _toComponent(
			String bomRef, List<CryptoUse> cryptoUses,
			Properties portalProperties)
		throws GeneralSecurityException {

		CryptoUse cryptoUse = cryptoUses.get(0);

		AlgorithmProperties algorithmProperties = new AlgorithmProperties();

		algorithmProperties.setCryptoFunctions(_getCryptoFunctions(cryptoUses));
		algorithmProperties.setPrimitive(cryptoUse.getPrimitive());

		CryptoProperties cryptoProperties = new CryptoProperties();

		cryptoProperties.setAlgorithmProperties(algorithmProperties);
		cryptoProperties.setAssetType(AssetType.ALGORITHM);

		Component component = new Component();

		component.setBomRef(bomRef);
		component.setCryptoProperties(cryptoProperties);
		component.setEvidence(_getEvidence(cryptoUses));
		component.setName(_getName(cryptoUse));
		component.setProperties(_getProperties(cryptoUses, portalProperties));
		component.setType(Component.Type.CRYPTOGRAPHIC_ASSET);

		return component;
	}

	private static void _validate(
		String bomRef, CryptoUse cryptoUse1, CryptoUse cryptoUse2) {

		if (Objects.equals(
				cryptoUse1.getConfiguredBy(), cryptoUse2.getConfiguredBy()) &&
			Objects.equals(
				cryptoUse1.getJCAClassNames(), cryptoUse2.getJCAClassNames()) &&
			(cryptoUse1.getPrimitive() == cryptoUse2.getPrimitive())) {

			return;
		}

		throw new IllegalArgumentException(
			StringBundler.concat(
				"Unable to merge the ", cryptoUse1.getFeatureName(), " and ",
				cryptoUse2.getFeatureName(), " uses of ", bomRef));
	}

}