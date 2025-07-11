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
package de.fraunhofer.iosb.app.aas.mapper;

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class SubmodelMapper extends ElementMapper implements Mapper<Submodel> {

    private final SubmodelElementMapper submodelElementMapper = new SubmodelElementMapper();
    private final Supplier<Boolean> onlySubmodelsDecision;

    public SubmodelMapper(Supplier<Boolean> onlySubmodelsDecision) {
        super();
        this.onlySubmodelsDecision = onlySubmodelsDecision;
    }

    @Override
    public Asset map(Submodel submodel, AasProvider provider) {
        var reference = createReference(KeyTypes.SUBMODEL, submodel.getId());
        Collection<Asset> children = new ArrayList<>();
        if (!onlySubmodelsDecision.get()) {
            children = submodel.getSubmodelElements().stream()
                    .map(elem -> submodelElementMapper.map(reference, elem, provider))
                    .toList();
        }

        var semanticId = Objects.requireNonNullElse(
                submodel.getSemanticId(),
                new DefaultReference.Builder().build());

        var dataAddress = createDataAddress(provider, reference);

        return mapIdentifiableToAssetBuilder(submodel)
                .id(getId(dataAddress))
                .properties(Map.of(
                        "semanticId", semanticId,
                        "submodelElements", children))
                .dataAddress(dataAddress)
                .build();
    }
}
