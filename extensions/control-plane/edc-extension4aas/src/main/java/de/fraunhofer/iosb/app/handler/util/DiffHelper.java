package de.fraunhofer.iosb.app.handler.util;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public abstract class DiffHelper {
    public static Map<PolicyBinding, Asset> getToAdd(Map<PolicyBinding, Asset> current, Map<PolicyBinding, Asset> updated) {
        return updated.entrySet().stream()
                .filter(entry -> current.values().stream().noneMatch(asset -> assetEqualityById(asset, entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public static Map<PolicyBinding, Asset> getToUpdate(Map<PolicyBinding, Asset> current, Map<PolicyBinding, Asset> updated) {
        return updated.entrySet().stream()
                // Get all that have an ID which already exists
                .filter(entry -> current.entrySet().stream()
                        .anyMatch(existing -> assetEqualityById(existing.getValue(), entry.getValue())))
                // Now filter away equals. What remains are metadata updates.
                .filter(entry -> current.values().stream().noneMatch(asset -> assetEqualityFull(asset, entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public static Map<PolicyBinding, Asset> getToRemove(Map<PolicyBinding, Asset> current, Map<PolicyBinding, Asset> updated) {
        return current.entrySet().stream()
                // Filter all "to remove"
                .filter(entry -> updated.values().stream().noneMatch(asset -> assetEqualityById(asset, entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    private static boolean assetEqualityById(Asset a, Asset b) {
        return Objects.equals(a.getId(), b.getId());
    }


    private static boolean dataAddressEquality(DataAddress a, DataAddress b) {
        return Objects.equals(a.getProperties(), b.getProperties());
    }


    private static boolean assetEqualityByDefaultValues(Asset a, Asset b) {
        return Objects.equals(a.getId(), b.getId()) &&
                Objects.equals(a.getVersion(), b.getVersion()) &&
                Objects.equals(a.getName(), b.getName()) &&
                Objects.equals(a.getContentType(), b.getContentType());
    }


    private static boolean assetEqualityFull(Asset a, Asset b) {
        return assetEqualityByDefaultValues(a, b) &&
                dataAddressEquality(a.getDataAddress(), b.getDataAddress()) &&
                Objects.equals(a.getProperties(), b.getProperties()) &&
                Objects.equals(a.getPrivateProperties(), b.getPrivateProperties());
    }
}
