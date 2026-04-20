/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.security.fips;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.Map;

/**
 * Emits structured FIPS 140-3 audit lines for integrity-check and key-generation
 * failures. The emitted line has the shape:
 *
 * <pre>
 * FIPS severity=&lt;level&gt; timestampMs=&lt;epoch ms&gt; operation=&lt;op&gt; feature=&lt;feature&gt; details={...}
 * </pre>
 *
 * <p>
 * <code>CRITICAL</code> lines go to {@code Log.error}; <code>WARN</code> lines go
 * to {@code Log.warn}. The line shape is deliberately stable so operators can
 * parse it from a log pipeline (e.g. SIEM forwarder).
 * </p>
 *
 * @author Caio Farias
 */
public class FIPSAuditLog {

	public static void logCritical(
		String operation, String feature, Map<String, Object> details) {

		_log.error(_format("CRITICAL", operation, feature, details));
	}

	public static void logWarn(
		String operation, String feature, Map<String, Object> details) {

		_log.warn(_format("WARN", operation, feature, details));
	}

	private static String _format(
		String severity, String operation, String feature,
		Map<String, Object> details) {

		StringBuilder sb = new StringBuilder();

		sb.append("FIPS severity=");
		sb.append(severity);
		sb.append(" timestampMs=");
		sb.append(System.currentTimeMillis());
		sb.append(" operation=");
		sb.append(operation);

		if (feature != null) {
			sb.append(" feature=");
			sb.append(feature);
		}

		if ((details != null) && !details.isEmpty()) {
			sb.append(" details=");
			sb.append(details);
		}

		return sb.toString();
	}

	private static final Log _log = LogFactoryUtil.getLog(FIPSAuditLog.class);

}
