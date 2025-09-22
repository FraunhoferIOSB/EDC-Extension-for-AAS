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
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

/**
 * Maps AAS {@code Submodel} -> EDC {@code Asset}, including its contained {@code SubmodelElements} as an {@code Asset}'s property (if the {@code
 * onlySubmodels} configuration variable is set to {@code false}).
 */
public class SubmodelMapper extends ElementMapper implements Mapper<Submodel> {

    private final SubmodelElementMapper submodelElementMapper = new SubmodelElementMapper();
    private final Supplier<Boolean> onlySubmodelsDecision;

    public SubmodelMapper(Supplier<Boolean> onlySubmodelsDecision) {
        this.onlySubmodelsDecision = onlySubmodelsDecision;
    }

    @Override
    public Asset apply(Submodel submodel, AasProvider provider) {
        Reference reference = AasUtils.toReference(submodel);
        DataAddress dataAddress = createDataAddress(provider, reference);

        Asset.Builder assetBuilder = mapIdentifiableToAssetBuilder(submodel)
                .id(getId(dataAddress))
                .dataAddress(dataAddress);

        // Add submodel Elements
        if (!onlySubmodelsDecision.get()
                && Objects.nonNull(submodel.getSubmodelElements())
                && !submodel.getSubmodelElements().isEmpty()) {

            Collection<Asset> children = submodel.getSubmodelElements().stream()
                    .map(elem -> submodelElementMapper.map(reference, elem, provider))
                    .toList();

            assetBuilder.property(AAS_V30_NAMESPACE + "Submodel/" + "submodelElements", children);
        }

        if (Objects.nonNull(submodel.getSemanticId())) {
            assetBuilder.property(AAS_V30_NAMESPACE + "HasSemantics/" + "semanticId", submodel.getSemanticId());
        }

        return assetBuilder.build();
    }
}
