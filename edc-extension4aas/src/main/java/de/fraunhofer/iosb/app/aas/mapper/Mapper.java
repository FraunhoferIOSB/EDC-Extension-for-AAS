package de.fraunhofer.iosb.app.aas.mapper;

import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

public interface Mapper<T extends Referable> {

    Asset map(T element, String accessUrl);
}
