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
package de.fraunhofer.iosb.ilt.dataspace.client.config;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;


@Settings
public class ClientConfiguration implements CatalogConfiguration, NegotiationConfiguration, TransferConfiguration {
    private static final String NAME = "DSP Client: ";

    private static final String CONTEXT = "edc.client.dsp";
    private static final String TIMEOUT = ".timeout";
    private static final String POLICY = ".policy";

    private static final String DEFAULT_TIMEOUT = "20";
    private static final String DEFAULT_ACCEPT_ALL_OFFERS = "false";

    @Setting(description = NAME + "catalog request timeout (seconds)", key = CONTEXT + TIMEOUT + ".catalog", required = false, defaultValue = DEFAULT_TIMEOUT)
    private Integer catalogTimeout;

    @Setting(description = NAME + "negotiation timeout (seconds)", key = CONTEXT + TIMEOUT + ".negotiation", required = false, defaultValue = DEFAULT_TIMEOUT)
    private Integer negotiationTimeout;

    @Setting(description = NAME + "transfer timeout (seconds)", key = CONTEXT + TIMEOUT + ".transfer", required = false, defaultValue = DEFAULT_TIMEOUT)
    private Integer transferTimeout;

    @Setting(description = NAME + "all policies accepted", key = CONTEXT + POLICY + ".accepted.all", required = false, defaultValue = DEFAULT_ACCEPT_ALL_OFFERS)
    private boolean allPoliciesAccepted;

    @Override
    public Integer getCatalogTimeout() {
        return catalogTimeout;
    }


    @Override
    public boolean acceptAllPolicies() {
        return allPoliciesAccepted;
    }


    @Override
    public Integer getNegotiationTimeout() {
        return negotiationTimeout;
    }


    @Override
    public Integer getTransferTimeout() {
        return transferTimeout;
    }
}
