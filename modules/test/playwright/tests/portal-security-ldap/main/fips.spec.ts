/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {ldapConfigurationPagesTest} from '../../../fixtures/ldapConfigurationPagesTest';
import {loginTest} from '../../../fixtures/loginTest';
import {clickAndExpectToBeVisible} from '../../../utils/clickAndExpectToBeVisible';
import getRandomString from '../../../utils/getRandomString';

export const test = mergeTests(loginTest(), ldapConfigurationPagesTest);

const LDAPS_URL_WITH_PORT_636_REGEX = /^ldaps:\/\/.*:636$/;

const PRESETS = [
	'Apache Directory Server',
	'Fedora Directory Server',
	'Microsoft Active Directory Server',
	'Novell eDirectory',
	'OpenLDAP',
];

test(
	'New LDAP server form defaults the base provider URL to the ldaps:// scheme and port 636',
	{tag: '@LPD-86301'},
	async ({ldapConfigurationPage, ldapServerPage}) => {
		await test.step('Open the Add LDAP Server form', async () => {
			await ldapConfigurationPage.addLdapServer();
		});

		await test.step('Assert the base provider URL defaults to ldaps:// and port 636', async () => {
			await ldapServerPage.serverName.waitFor();

			await expect(ldapServerPage.baseProviderUrl).toHaveValue(LDAPS_URL_WITH_PORT_636_REGEX);
		});
	}
);

test(
	'Selecting a preset applies the ldaps:// scheme and port 636 to the base provider URL',
	{tag: '@LPD-86301'},
	async ({ldapConfigurationPage, ldapServerPage}) => {
		await test.step('Open the Add LDAP Server form', async () => {
			await ldapConfigurationPage.addLdapServer();

			await ldapServerPage.serverName.waitFor();
		});

		for (const preset of PRESETS) {
			await test.step(`Assert the ${preset} preset uses ldaps:// and port 636`, async () => {
				await ldapServerPage.page
					.getByText(preset)
					.getByRole('radio')
					.check();

				await expect(ldapServerPage.baseProviderUrl).toHaveValue(LDAPS_URL_WITH_PORT_636_REGEX);
			});
		}
	}
);

test(
	'Saving an LDAP server with an insecure ldap:// base provider URL shows the FIPS validation alert and blocks submission',
	{tag: '@LPD-86301'},
	async ({ldapConfigurationPage, ldapServerPage}) => {
		const serverName = `fips-${getRandomString()}`;

		await test.step('Open the Add LDAP Server form', async () => {
			await ldapConfigurationPage.addLdapServer();
		});

		await test.step('Fill the form with an insecure ldap:// URL', async () => {
			await ldapServerPage.serverName.waitFor();

			await ldapServerPage.serverName.fill(serverName);
			await ldapServerPage.baseProviderUrl.fill(
				`ldap://${getRandomString()}`
			);
		});

		await test.step('Submit the form and assert the FIPS validation alert is shown', async () => {
			await clickAndExpectToBeVisible({
				target: ldapServerPage.page
					.getByRole('dialog')
					.getByText(
						'The base provider URL must use the "ldaps://" scheme in FIPS mode.'
					),
				trigger: ldapServerPage.saveButton,
			});
		});

		await test.step('Assert the form is still visible so submission was blocked', async () => {
			await expect(ldapServerPage.serverName).toBeVisible();
			await expect(ldapServerPage.baseProviderUrl).toBeVisible();
		});
	}
);
