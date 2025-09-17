package de.fraunhofer.iosb.aas.lib.util;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;

import java.util.List;

public final class AasReferenceUtil {

    public static Reference toReference(AssetAdministrationShell shell) {
        return getReferenceBuilder()
                .keys(new DefaultKey.Builder()
                        .type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
                        .value(shell.getId())
                        .build())
                .build();
    }

    public static Reference toReference(Submodel submodel) {
        return getReferenceBuilder()
                .keys(new DefaultKey.Builder()
                        .type(KeyTypes.SUBMODEL)
                        .value(submodel.getId())
                        .build())
                .build();
    }

    public static Reference toReference(Submodel submodel, String idShortPath) {
        return getReferenceBuilder()
                .keys(List.of(
                                new DefaultKey.Builder()
                                        .type(KeyTypes.SUBMODEL)
                                        .value(submodel.getId())
                                        .build(),
                                new DefaultKey.Builder()
                                        .type(KeyTypes.SUBMODEL_ELEMENT)
                                        .value(idShortPath)
                                        .build()
                        )
                )
                .build();
    }

    private static DefaultReference.Builder getReferenceBuilder() {
        return new DefaultReference.Builder()
                .type(ReferenceTypes.MODEL_REFERENCE);
    }
}
