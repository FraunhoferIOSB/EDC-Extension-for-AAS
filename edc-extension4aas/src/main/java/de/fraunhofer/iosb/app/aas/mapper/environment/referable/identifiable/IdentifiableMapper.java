package de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable;

import de.fraunhofer.iosb.app.aas.mapper.environment.referable.ReferableMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public abstract class IdentifiableMapper extends ReferableMapper {
    private static final String IDENTIFIABLE_NAMESPACE = AAS_V30_NAMESPACE.concat("Identifiable/");

    protected Asset.Builder map(Identifiable identifiable) {
        Asset.Builder builder = super.map(identifiable);

        builder.property(IDENTIFIABLE_NAMESPACE.concat("id"), identifiable.getId());

        var admin = identifiable.getAdministration();

        if (admin == null) {
            return builder;
        }

        if (admin.getEmbeddedDataSpecifications() != null && !admin.getEmbeddedDataSpecifications().isEmpty()) {
            builder.property(AAS_V30_NAMESPACE + "HasDataSpecification/" + "embeddedDataSpecifications", admin.getEmbeddedDataSpecifications());
        }

        if (admin.getVersion() != null && !admin.getVersion().isEmpty()) {
            builder.property(AAS_V30_NAMESPACE + "AdministrativeInformation/" + "version", admin.getVersion());
        }

        if (admin.getRevision() != null && !admin.getRevision().isEmpty()) {
            builder.property(AAS_V30_NAMESPACE + "AdministrativeInformation/" + "revision", admin.getRevision());
        }

        return builder;
    }
}
