/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.cluster.multiple.internal.jgroups;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.cluster.multiple.configuration.ClusterExecutorConfiguration;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.test.util.PropsValuesTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;

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
	public void testCreateClusterChannel() {
		JGroupsClusterChannelFactory jGroupsClusterChannelFactory =
			new JGroupsClusterChannelFactory(
				ConfigurableUtil.createConfigurable(
					ClusterExecutorConfiguration.class,
					Collections.emptyMap()));

		try (SafeCloseable safeCloseable =
				PropsValuesTestUtil.swapWithSafeCloseable(
					"FIPS_ENABLED", true)) {

			_testCreateClusterChannel(
				"jgroups/secure/md5/udp_control.xml",
				jGroupsClusterChannelFactory);
			_testCreateClusterChannel(
				"jgroups/secure/md5/udp_transport.xml",
				jGroupsClusterChannelFactory);
			_testCreateClusterChannel(
				"jgroups/unsecure/udp_control.xml",
				jGroupsClusterChannelFactory);
			_testCreateClusterChannel(
				"jgroups/unsecure/udp_transport.xml",
				jGroupsClusterChannelFactory);
		}
	}

	private void _testCreateClusterChannel(
		String channelPropertiesLocation,
		JGroupsClusterChannelFactory jGroupsClusterChannelFactory) {

		SecurityException securityException = Assert.assertThrows(
			SecurityException.class,
			() -> jGroupsClusterChannelFactory.createClusterChannel(
				null, null, channelPropertiesLocation, null, null));

		Assert.assertEquals(
			"The clustering authentication profile \"" +
				channelPropertiesLocation + "\" is not allowed in FIPS mode",
			securityException.getMessage());
	}

}