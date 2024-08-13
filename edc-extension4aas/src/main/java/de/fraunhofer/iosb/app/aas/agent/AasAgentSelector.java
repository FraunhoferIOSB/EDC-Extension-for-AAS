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
package de.fraunhofer.iosb.app.aas.agent;

import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.result.AbstractResult;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionMetaInformation;


/**
 * Selects adequate AAS agent from collection of agents best fitting for the given input type
 */
public class AasAgentSelector extends PipelineStep<Set<SelfDescriptionMetaInformation>,
        Map<URL, Environment>> {

    private final Collection<AasAgent> agents = new ArrayList<>();

    public AasAgentSelector(@NotNull Collection<AasAgent> agents) {
        this.agents.addAll(agents);
    }

    @Override
    public PipelineResult<Map<URL, Environment>> apply(Set<SelfDescriptionMetaInformation> metaInformation) {
        // First, exclude registries' services from the synchronization process. Otherwise, all the services stored in the registries would get treated as normal services.
        var results = metaInformation.stream().map(this::executeSingle).toList();

        if (results.stream().anyMatch(AbstractResult::failed)) {
            return PipelineResult.failure(
                    PipelineFailure.fatal(
                            results.stream()
                                    .map(AbstractResult::getFailureMessages)
                                    .flatMap(List::stream)
                                    .toList()));
        }

        return PipelineResult.success(
                results.stream()
                        .map(AbstractResult::getContent)
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public PipelineResult<Map<URL, Environment>> executeSingle(SelfDescriptionMetaInformation metaInformation) {
        var agent = agents.stream().filter(item -> metaInformation.type().equals(item.supportedType())).findAny();

        if (agent.isPresent()) {
            return agent.get().apply(metaInformation.url());
        } else {
            return PipelineResult.failure(
                    PipelineFailure.fatal(
                            List.of("No AAS agent for type %s found".formatted(metaInformation.type()))));
        }
    }

}
