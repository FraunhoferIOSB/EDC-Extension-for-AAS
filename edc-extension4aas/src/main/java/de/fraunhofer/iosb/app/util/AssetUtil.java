package de.fraunhofer.iosb.app.util;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AssetUtil {
    private AssetUtil() {
        throw new RuntimeException("Utility class");
    }

    /**
     * Flatten nested asset into collection of all containing assets.
     * The top asset will not be returned within this list.
     *
     * @param environmentAsset Representing the environment
     * @return Flattened collection of all assets
     */
    public static Collection<Asset> flatMapAssets(Asset environmentAsset) {
        var assets = getChildren(environmentAsset, "shells");
        assets.addAll(getChildren(environmentAsset, "conceptDescriptions"));

        var submodels = getChildren(environmentAsset, "submodels");
        assets.addAll(submodels);
        assets.addAll(submodels.stream()
                .map(submodel -> getChildren(submodel, "submodelElements"))
                .flatMap(Collection::stream)
                .map(AssetUtil::getChildrenRec)
                .flatMap(Collection::stream)
                .toList());

        return assets;
    }

    private static Collection<Asset> getChildrenRec(Asset parent) {
        var children = getChildren(parent, "value");
        if (!children.isEmpty()) {
            var grandChildren = children.stream().map(AssetUtil::getChildrenRec).flatMap(Collection::stream).toList();
            children.addAll(grandChildren);
        }
        return children;
    }

    @SuppressWarnings("unchecked") // i checked
    public static Collection<Asset> getChildren(Asset parent, String childPropertyName) {
        var childrenMaybe = parent.getProperty(childPropertyName);
        if (childrenMaybe instanceof List && !((List<?>) childrenMaybe).isEmpty() && ((List<?>) childrenMaybe).get(0) instanceof Asset) {
            return new ArrayList<>((List<Asset>) childrenMaybe);
        }
        return new ArrayList<>();
    }
}
