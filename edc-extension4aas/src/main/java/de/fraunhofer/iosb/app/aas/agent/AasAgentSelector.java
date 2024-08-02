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

import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.AbstractResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AasAgentSelector extends PipelineStep<Set<SelfDescriptionRepository.SelfDescriptionMetaInformation>, Map<String, Environment>> {

    private final Collection<AasAgent> agents;

    public AasAgentSelector(Collection<AasAgent> agents) {
        this.agents = agents;
    }

    @Override
    public PipelineResult<Map<String, Environment>> execute(Set<SelfDescriptionRepository.SelfDescriptionMetaInformation> metaInformation) throws Exception {

        var results = metaInformation.stream().map(this::executeSingle).toList();

        if (results.stream().anyMatch(AbstractResult::failed)) {
            return PipelineResult.failure(
                    new PipelineFailure(results.stream().map(res -> res.getFailure().getMessages()).flatMap(List::stream).toList(),
                            PipelineFailure.PipelineFailureType.FATAL));
        }


        return PipelineResult.success(results.stream()
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public PipelineResult<Map<String, Environment>> executeSingle(SelfDescriptionRepository.SelfDescriptionMetaInformation metaInformation) {
        var agent = agents.stream().filter(item -> metaInformation.type().equals(item.supportedType())).findAny();

        if (agent.isPresent()) {
            try {
                return agent.get().execute(metaInformation.url());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new EdcException("No AAS agent for type %s found".formatted(metaInformation.type()));
        }
    }

}
