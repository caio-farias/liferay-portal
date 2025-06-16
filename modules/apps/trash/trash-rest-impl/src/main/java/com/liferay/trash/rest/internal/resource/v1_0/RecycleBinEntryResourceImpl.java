/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.trash.rest.internal.resource.v1_0;

import com.liferay.trash.rest.resource.v1_0.RecycleBinEntryResource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Manuele Castro
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/recycle-bin-entry.properties",
	scope = ServiceScope.PROTOTYPE, service = RecycleBinEntryResource.class
)
public class RecycleBinEntryResourceImpl
	extends BaseRecycleBinEntryResourceImpl {
}