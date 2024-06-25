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
package de.fraunhofer.iosb.app.edc;

import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ContractHandlerTest {

    private static final String DEFAULT_CONTRACT_NAME = "DEFAULT_CONTRACT";
    private final ContractDefinitionStore mockedContractDefinitionStore = mock(ContractDefinitionStore.class);
    private final PolicyDefinitionStore mockedPolicyDefinitionStore = mock(PolicyDefinitionStore.class);
    private ContractHandler contractHandler;

    @BeforeEach
    public void initializeAasAgent() {
        contractHandler = new ContractHandler(mockedContractDefinitionStore, mockedPolicyDefinitionStore, new ConsoleMonitor());
    }

    @Test
    public void registerAssetToDefaultContractTest() {
        assertEquals(format("%s1", DEFAULT_CONTRACT_NAME),
                contractHandler.registerAssetToDefaultContract("test-asset"));
        verify(mockedContractDefinitionStore, times(1)).save(any());
        verify(mockedPolicyDefinitionStore, times(2)).create(any());
    }

}
