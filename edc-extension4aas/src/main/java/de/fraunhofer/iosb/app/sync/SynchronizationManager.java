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
package de.fraunhofer.iosb.app.sync;

import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.edc.spi.EdcException;

public class SynchronizationManager implements SelfDescriptionChangeListener, Runnable {

    private static final String NO_SYNCHRONIZER_FOR_TYPE_FOUND = "No synchronizer for type %s found";

    private final SelfDescriptionRepository selfDescriptionRepository;
    private final SynchronizerRepository synchronizerRepository;
    private final AasServiceRegistry aasServiceRegistry;

    SynchronizationManager(SelfDescriptionRepository selfDescriptionRepository,
                           AasServiceRegistry aasServiceRegistry, SynchronizerRepository synchronizerRepository) {
        this.selfDescriptionRepository = selfDescriptionRepository;
        this.aasServiceRegistry = aasServiceRegistry;
        this.synchronizerRepository = synchronizerRepository;
    }

    /**
     * Synchronize AAS services with self-description and EDC AssetIndex/ContractStore
     */
    @Override
    public void run() {
        selfDescriptionRepository.getAllSelfDescriptionMetaInformation().forEach(metaInformation -> synchronizerRepository.getSynchronizers().stream()
                .filter(synchronizer -> synchronizer.supportedType().equals(metaInformation.type()))
                .findFirst()
                .orElseThrow(() -> new EdcException(
                        NO_SYNCHRONIZER_FOR_TYPE_FOUND.formatted(metaInformation.type())))
                .synchronize(metaInformation.url()));
    }

    @Override
    public void created(SelfDescriptionRepository.SelfDescriptionMetaInformation metaInformation) {
        var registrationResult = aasServiceRegistry.register(metaInformation.url().toString());
        if (registrationResult.failed()) {
            throw new EdcException("Could not synchronize %s: %s".formatted(
                    metaInformation.url(), registrationResult.getFailureMessages()));
        }

        // Synchronize now
        run();
    }

    @Override
    public void removed(SelfDescriptionRepository.SelfDescriptionMetaInformation metaInformation) {
        synchronizerRepository.getSynchronizers().stream()
                .filter(synchronizer -> synchronizer.supportedType().equals(metaInformation.type()))
                .findFirst()
                .orElseThrow(() -> new EdcException(
                        NO_SYNCHRONIZER_FOR_TYPE_FOUND.formatted(metaInformation.type())))
                .remove(metaInformation.url());

        aasServiceRegistry.unregister(metaInformation.url().toString());
    }

    public static class Builder {
        private SelfDescriptionRepository selfDescriptionRepository;
        private AasServiceRegistry aasServiceRegistry;
        private SynchronizerRepository synchronizerRepository;

        private Builder(){
        }

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder selfDescriptionRepository(SelfDescriptionRepository selfDescriptionRepository) {
            this.selfDescriptionRepository = selfDescriptionRepository;
            return this;
        }

        public Builder aasServiceRegistry(AasServiceRegistry aasServiceRegistry) {
            this.aasServiceRegistry = aasServiceRegistry;
            return this;
        }

        public SynchronizationManager build() {
            return new SynchronizationManager(selfDescriptionRepository, aasServiceRegistry, synchronizerRepository);
        }

        public Builder synchronizerRepository(SynchronizerRepository synchronizerRepository) {
            this.synchronizerRepository = synchronizerRepository;
            return this;

        }
    }
}
