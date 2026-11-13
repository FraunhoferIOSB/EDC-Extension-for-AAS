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
package de.fraunhofer.iosb.ilt.dataspace.client;

import de.fraunhofer.iosb.ilt.dataspace.client.config.ClientConfiguration;
import de.fraunhofer.iosb.ilt.dataspace.client.negotiation.Negotiator;
import de.fraunhofer.iosb.ilt.dataspace.client.policy.AcceptedPolicyStore;
import de.fraunhofer.iosb.ilt.dataspace.client.policy.PolicyController;
import de.fraunhofer.iosb.ilt.dataspace.client.policy.PolicyService;
import de.fraunhofer.iosb.ilt.dataspace.client.store.CounterPartyAddressStore;
import de.fraunhofer.iosb.ilt.dataspace.client.transfer.pull.PullTransferInitiator;
import de.fraunhofer.iosb.ilt.dataspace.client.util.DefaultingParticipantContextStore;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;


/** EDC service extension providing the automated contract negotiation client. */
public class DspClientExtension implements ServiceExtension {

    /** Default constructor. */
    public DspClientExtension() {}

    @Configuration
    private ClientConfiguration configuration;

    @Inject
    private CatalogService catalogService;
    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private ContractNegotiationService contractNegotiationService;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private ParticipantContextStore participantContextStore;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private TransferProcessObservable transferProcessObservable;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;
    @Inject
    private DataAddressStore dataAddressStore;
    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor().withPrefix("DSP-Client");
        var dspApiTransformerRegistry = typeTransformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1);

        CounterPartyAddressStore counterPartyAddressStore = new CounterPartyAddressStore(contractNegotiationStore);
        ParticipantContextStore defaultingParticipantContextStore = new DefaultingParticipantContextStore(participantContextStore, context.getConfig());
        AcceptedPolicyStore acceptedPolicyStore = new AcceptedPolicyStore();

        PolicyService policyService = new PolicyService(catalogService, participantContextStore, dspApiTransformerRegistry, configuration,
                acceptedPolicyStore, jsonLd, counterPartyAddressStore);

        Negotiator negotiator = new Negotiator(contractNegotiationService, contractNegotiationStore, defaultingParticipantContextStore,
                counterPartyAddressStore, contractNegotiationObservable, configuration);

        PullTransferInitiator pullTransferInitiator = new PullTransferInitiator(transferProcessService, defaultingParticipantContextStore, counterPartyAddressStore,
                transferProcessObservable, monitor, configuration);

        var policyController = new PolicyController(acceptedPolicyStore);
        var dspClientController = new DspClientController(monitor, policyService, negotiator, pullTransferInitiator);

        webService.registerResource(policyController);
        webService.registerResource(dspClientController);
    }
}
