/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.ilt.dataspace.seed;

import de.fraunhofer.iosb.ilt.dataspace.seed.util.RsaKeyUtils;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

import java.security.GeneralSecurityException;


public class ProviderSeedExtension implements ServiceExtension {

    @Inject(required = false)
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        prepareTxDataPlanePull(context.getConfig(), vault, context.getMonitor().withPrefix(this.getClass().getSimpleName()));
    }


    private static void prepareTxDataPlanePull(Config config, Vault vault, Monitor monitor) {
        boolean doSeed = config.getBoolean("edc.provider.seed.proxy.token", false);
        String publicKeyAlias = config.getString("edc.transfer.proxy.token.verifier.publickey.alias", null);
        String privateKeyAlias = config.getString("edc.transfer.proxy.token.signer.privatekey.alias", null);
        String participantContextId = config.getString("edc.participant.context.id", config.getString("edc.participant.id", null));
        if (doSeed || (vault instanceof InMemoryVault && publicKeyAlias != null && privateKeyAlias != null && participantContextId != null)) {
            monitor.warning("Seeding enforced or InMemoryVault was used in combination with edc.transfer.proxy.token functionality. Created keypair and stored it in vault.");
            RsaKeyUtils.PemKeys keys;
            try {
                keys = RsaKeyUtils.generate();
            }
            catch (GeneralSecurityException e) {
                monitor.warning(
                        "Seeding enforced or InMemoryVault was used in combination with edc.transfer.proxy.token functionality. Exception occurred trying to create keypair.", e);
                return;
            }

            vault.storeSecret(participantContextId, privateKeyAlias, keys.privateKeyPkcs8Pem());
            vault.storeSecret(participantContextId, publicKeyAlias, keys.publicKeyPem());
        }
        else {
            monitor.info("Did not seed vault.");
        }
    }
}
