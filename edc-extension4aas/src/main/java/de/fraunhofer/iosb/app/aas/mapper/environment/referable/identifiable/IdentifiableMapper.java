package de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable;

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.app.aas.mapper.environment.referable.ReferableMapper;
import de.fraunhofer.iosb.app.aas.mapper.environment.referable.SubmodelElementMapper;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public class IdentifiableMapper extends ReferableMapper {

    public static final String SUBMODEL_ELEMENT_LOCATION = AAS_V30_NAMESPACE + "Submodel/" + "submodelElements";
    private static final String IDENTIFIABLE_NAMESPACE = AAS_V30_NAMESPACE.concat("Identifiable/");
    private final Supplier<Boolean> onlySubmodelsDecision = () -> Configuration.getInstance().onlySubmodels();
    private final SubmodelElementMapper submodelElementMapper = new SubmodelElementMapper();


    public Asset map(Identifiable identifiable, AasProvider provider) {
        Reference reference = AasUtils.toReference(identifiable);

        var dataAddress = createDataAddress(provider, reference);

        Asset.Builder builder = super.map(identifiable)
                .id(generateId(dataAddress))
                .dataAddress(dataAddress);

        builder.property(IDENTIFIABLE_NAMESPACE.concat("id"), identifiable.getId());

        // Add submodel Elements
        if (identifiable instanceof Submodel submodel && !onlySubmodelsDecision.get() &&
                Objects.nonNull(submodel.getSubmodelElements()) && !submodel.getSubmodelElements().isEmpty()) {

            Collection<Asset> children = submodel.getSubmodelElements().stream()
                    .map(elem -> submodelElementMapper.map(reference, elem, provider))
                    .toList();

            builder.property(SUBMODEL_ELEMENT_LOCATION, children);
        }

        var admin = identifiable.getAdministration();

        if (admin != null) {
            builder.property(IDENTIFIABLE_NAMESPACE.concat("administration"), getNamespaced(admin));
        }

        return builder.build();
    }
}
