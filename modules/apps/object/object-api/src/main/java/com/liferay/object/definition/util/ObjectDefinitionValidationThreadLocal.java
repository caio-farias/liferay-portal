/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.definition.util;

import com.liferay.object.exception.ObjectDefinitionValidationException;
import com.liferay.petra.lang.CentralizedThreadLocal;

import java.util.List;

/**
 * @author Caio Farias
 */
public class ObjectDefinitionValidationThreadLocal {

	public static String getObjectDefinitionExternalReferenceCode() {
		return _objectDefinitionExternalReferenceCode.get();
	}

	public static List<ObjectDefinitionValidationException.ValidationError>
		getValidationErrors() {

		return _validationErrors.get();
	}

	public static <E extends Exception> void handleException(
			String className, E exception, String property, Object value)
		throws E {

		if (!_accumulateError.get()) {
			throw exception;
		}

		List<ObjectDefinitionValidationException.ValidationError>
			validationErrors = _validationErrors.get();

		if (validationErrors == null) {
			return;
		}

		Class<?> clazz = exception.getClass();

		validationErrors.add(
			new ObjectDefinitionValidationException.ValidationError(
				className, exception.getMessage(), clazz.getName(), property,
				value));

		_validationErrors.set(validationErrors);
	}

	public static boolean hasValidationErrors() {
		List<ObjectDefinitionValidationException.ValidationError>
			validationErrors = _validationErrors.get();

		if (!_accumulateError.get() || (validationErrors == null)) {
			return false;
		}

		return !validationErrors.isEmpty();
	}

	public static boolean isAccumulateError() {
		return _accumulateError.get();
	}

	public static void setAccumulateError(boolean accumulateError) {
		_accumulateError.set(accumulateError);
	}

	public static void setObjectDefinitionExternalReferenceCode(
		String objectDefinitionExternalReferenceCode) {

		_objectDefinitionExternalReferenceCode.set(
			objectDefinitionExternalReferenceCode);
	}

	public static void setValidationErrors(
		List<ObjectDefinitionValidationException.ValidationError>
			validationErrors) {

		_validationErrors.set(validationErrors);
	}

	private static final CentralizedThreadLocal<Boolean> _accumulateError =
		new CentralizedThreadLocal<>(
			ObjectDefinitionValidationThreadLocal.class + "._accumulateError",
			() -> Boolean.FALSE);
	private static final CentralizedThreadLocal<String>
		_objectDefinitionExternalReferenceCode = new CentralizedThreadLocal<>(
			ObjectDefinitionValidationThreadLocal.class +
				"._objectDefinitionExternalReferenceCode");
	private static final CentralizedThreadLocal
		<List<ObjectDefinitionValidationException.ValidationError>>
			_validationErrors = new CentralizedThreadLocal<>(
				ObjectDefinitionValidationThreadLocal.class +
					"._validationErrors");

}