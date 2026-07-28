/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.cluster.multiple.internal.jgroups;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.cluster.multiple.configuration.ClusterExecutorConfiguration;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Caio Farias
 */
public class JGroupsClusterChannelFactoryTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testAsymKeyLength() throws Exception {
		for (String channelPropertiesLocation :
				ArrayUtil.append(
					_CHANNEL_PROPERTIES_LOCATIONS_MD5,
					_CHANNEL_PROPERTIES_LOCATIONS_X509)) {

			Matcher matcher = _asymKeyLengthPattern.matcher(
				StringUtil.read(
					JGroupsClusterChannelFactoryTest.class.getClassLoader(),
					channelPropertiesLocation));

			Assert.assertTrue(channelPropertiesLocation, matcher.find());
			Assert.assertTrue(
				channelPropertiesLocation,
				GetterUtil.getInteger(matcher.group(1)) >= 2048);
		}
	}

	@Test
	public void testCreateClusterChannel() throws Exception {
		JGroupsClusterChannelFactory jGroupsClusterChannelFactory =
			new JGroupsClusterChannelFactory(
				ConfigurableUtil.createConfigurable(
					ClusterExecutorConfiguration.class,
					Collections.emptyMap()));

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", false)) {

			Assert.assertThrows(
				SystemException.class,
				() -> jGroupsClusterChannelFactory.createClusterChannel(
					null, null, _CHANNEL_PROPERTIES_LOCATIONS_MD5[0], null,
					null));
		}

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			for (String channelPropertiesLocation :
					ArrayUtil.append(
						_CHANNEL_PROPERTIES_LOCATIONS_MD5,
						_CHANNEL_PROPERTIES_LOCATIONS_UNSECURE)) {

				SecurityException securityException = Assert.assertThrows(
					SecurityException.class,
					() -> jGroupsClusterChannelFactory.createClusterChannel(
						null, null, channelPropertiesLocation, null, null));

				Assert.assertEquals(
					"The clustering authentication profile \"" +
						channelPropertiesLocation +
							"\" is not allowed in FIPS mode",
					securityException.getMessage());
			}

			for (String channelPropertiesLocation :
					_CHANNEL_PROPERTIES_LOCATIONS_X509) {

				Assert.assertThrows(
					SystemException.class,
					() -> jGroupsClusterChannelFactory.createClusterChannel(
						null, null, channelPropertiesLocation, null, null));
			}
		}
	}

	private static final String[] _CHANNEL_PROPERTIES_LOCATIONS_MD5 = {
		"jgroups/secure/md5/udp_control.xml",
		"jgroups/secure/md5/udp_transport.xml"
	};

	private static final String[] _CHANNEL_PROPERTIES_LOCATIONS_UNSECURE = {
		"jgroups/unsecure/udp_control.xml", "jgroups/unsecure/udp_transport.xml"
	};

	private static final String[] _CHANNEL_PROPERTIES_LOCATIONS_X509 = {
		"jgroups/secure/x509/udp_control.xml",
		"jgroups/secure/x509/udp_transport.xml"
	};

	private static final Pattern _asymKeyLengthPattern = Pattern.compile(
		"asym_keylength=\"([0-9]+)\"");

}