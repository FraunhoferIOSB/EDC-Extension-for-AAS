package de.fraunhofer.iosb.app.aas.mapper.environment.referable;

import de.fraunhofer.iosb.app.aas.mapper.ElementMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public abstract class ReferableMapper extends ElementMapper {
    public static final String CONTENT_TYPE = "application/json";
    private static final String REFERABLE_NAMESPACE = AAS_V30_NAMESPACE.concat("Referable/");

    protected Asset.Builder map(Referable referable) {
        var assetBuilder = Asset.Builder.newInstance();

        if (referable.getIdShort() != null && !referable.getIdShort().isEmpty()) {
            assetBuilder.property(REFERABLE_NAMESPACE.concat("idShort"), referable.getIdShort());
        }

        if (referable.getDisplayName() != null && !referable.getDisplayName().isEmpty()) {
            assetBuilder.property(REFERABLE_NAMESPACE.concat("displayName"), referable.getDisplayName());
        }

        if (referable.getDescription() != null && !referable.getDescription().isEmpty()) {
            assetBuilder.property(REFERABLE_NAMESPACE.concat("description"), referable.getDescription());
        }

        return assetBuilder.contentType(CONTENT_TYPE);
    }
}
