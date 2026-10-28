/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.security.fips.cbom.builder;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import org.cyclonedx.model.component.crypto.enums.CryptoFunction;
import org.cyclonedx.model.component.crypto.enums.Primitive;

/**
 * @author Manuele Castro
 */
public class CBOMBuilder {

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

}