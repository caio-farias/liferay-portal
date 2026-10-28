/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropertiesUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

/**
 * @author Manuele Castro
 */
public class CBOMBuilder {

	public static String build(
			String configurationName, List<CryptoUse> cryptoUses,
			Properties portalProperties)
		throws Exception {

		List<Component> components = CryptoAssetComponentUtil.toComponents(
			cryptoUses, portalProperties);

		components.sort(Comparator.comparing(Component::getBomRef));

		return CycloneDXCBOMWriter.write(
			components, configurationName, ReleaseInfo.getName(),
			ReleaseInfo.getVersionDisplayName());
	}

	public static List<CryptoUse> getCryptoUses() throws Exception {
		List<CryptoUse> cryptoUses = new ArrayList<>();

		String json = StringUtil.read(
			CBOMBuilder.class, "dependencies/crypto-uses.json");

		JSONArray featuresJSONArray = JSONFactoryUtil.createJSONArray(json);

		for (int i = 0; i < featuresJSONArray.length(); i++) {
			JSONObject featureJSONObject = featuresJSONArray.getJSONObject(i);

			JSONArray usesJSONArray = featureJSONObject.getJSONArray("uses");

			for (int j = 0; j < usesJSONArray.length(); j++) {
				JSONObject useJSONObject = usesJSONArray.getJSONObject(j);

				String algorithm = useJSONObject.getString("algorithm", null);
				List<String> classNames = JSONUtil.toStringList(
					useJSONObject.getJSONArray("classNames"));
				List<String> configuredBy = JSONUtil.toStringList(
					useJSONObject.getJSONArray("configuredBy"));

				List<String> cryptoFunctionNames = JSONUtil.toStringList(
					useJSONObject.getJSONArray("cryptoFunctions"));

				List<CryptoFunction> cryptoFunctions = TransformUtil.transform(
					cryptoFunctionNames, CryptoFunction::valueOf);

				String featureName = featureJSONObject.getString("name");
				List<String> jcaClassNames = JSONUtil.toStringList(
					useJSONObject.getJSONArray("jcaClassNames"));
				Primitive primitive = Primitive.valueOf(
					useJSONObject.getString("primitive"));

				cryptoUses.add(
					new CryptoUse(
						algorithm, classNames, configuredBy, cryptoFunctions,
						featureName, jcaClassNames, primitive));
			}
		}

		return cryptoUses;
	}

	public static void main(String[] args) throws Exception {
		File configurationDir = new File(args[0]);

		Properties portalExtProperties = _loadProperties(
			new File(configurationDir, "portal-ext.properties"));

		if (GetterUtil.getBoolean(
				portalExtProperties.getProperty("fips.enabled"))) {

			throw new UnsupportedOperationException(
				"Unable to build the CBOM of a FIPS mode configuration");
		}

		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(new JSONFactoryImpl());

		String portalPropertiesContent = StringUtil.read(
			CBOMBuilder.class.getClassLoader(), "portal.properties");

		Properties portalProperties = PropertiesUtil.load(
			portalPropertiesContent);

		String cbomJSON = build(
			configurationDir.getName(), getCryptoUses(), portalProperties);

		File file = new File(args[1]);

		File parentDir = file.getParentFile();

		parentDir.mkdirs();

		Files.write(file.toPath(), cbomJSON.getBytes(StandardCharsets.UTF_8));

		System.out.println("Wrote " + file.getAbsolutePath());
	}

	private static Properties _loadProperties(File file) throws Exception {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			return PropertiesUtil.load(fileInputStream, StringPool.UTF8);
		}
	}

}