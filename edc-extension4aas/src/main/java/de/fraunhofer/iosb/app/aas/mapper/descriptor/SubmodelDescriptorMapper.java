package de.fraunhofer.iosb.app.aas.mapper.descriptor;

import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.ProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityAttributeObject;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityTypeEnum;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public class SubmodelDescriptorMapper extends DescriptorMapper {
    private static final String SUBMODEL_DESCRIPTOR_NAMESPACE = AAS_V30_NAMESPACE.concat("/SubmodelDescriptor/");
    private final List<String> supportedProtocols = List.of("HTTP", "HTTPS", "http", "https");

    public Asset map(SubmodelDescriptor descriptor) throws MalformedURLException {
        DataAddress dataAddress = createDataAddress(new URL(getHref(descriptor.getEndpoints())));
        return super.map(descriptor)
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("administration"), descriptor.getAdministration())
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("idShort"), descriptor.getIdShort())
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("id"), descriptor.getId())
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("semanticId"), descriptor.getSemanticId())
                .dataAddress(dataAddress)
                .id(generateId(dataAddress))
                .build();
    }


    private String getHref(List<Endpoint> endpoints) {
        return endpoints.stream()
                .filter(endpoint -> endpoint.get_interface().startsWith("SUBMODEL"))
                .filter(endpoint -> endpoint.get_interface().endsWith("3.0"))
                .map(Endpoint::getProtocolInformation)
                .filter(protocolInformation -> supportedProtocols.contains(protocolInformation.getEndpointProtocol()))
                // Remove endpoints which require security
                .filter(protocolInformation -> protocolInformation.getSecurityAttributes().stream()
                        .map(SecurityAttributeObject::getType)
                        .anyMatch(type -> type.equals(SecurityTypeEnum.NONE)))
                .map(ProtocolInformation::getHref)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No suitable endpoints found."));
    }
}
