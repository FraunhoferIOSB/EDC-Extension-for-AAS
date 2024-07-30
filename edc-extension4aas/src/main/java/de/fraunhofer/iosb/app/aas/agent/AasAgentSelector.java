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
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.EdcException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AasAgentSelector extends PipelineStep<Set<SelfDescriptionRepository.SelfDescriptionMetaInformation>, Map<String, Environment>> {

    private final Collection<AasAgent> agents;

    public AasAgentSelector(Collection<AasAgent> agents) {
        this.agents = agents;
    }

    @Override
    public Map<String, Environment> execute(Set<SelfDescriptionRepository.SelfDescriptionMetaInformation> metaInformation) throws Exception {
        var results = new HashMap<String, Environment>();
        for (var info : metaInformation) {
            results.putAll(executeSingle(info));
        }
        return results;
    }

    public Map<String, Environment> executeSingle(SelfDescriptionRepository.SelfDescriptionMetaInformation metaInformation) throws Exception {
        var agent = agents.stream().filter(item -> metaInformation.type().equals(item.supportedType())).findAny();

        if (agent.isPresent()) {
            return agent.get().execute(metaInformation.url());
        } else {
            throw new EdcException("No AAS agent for type %s found".formatted(metaInformation.type()));
        }
    }

}
