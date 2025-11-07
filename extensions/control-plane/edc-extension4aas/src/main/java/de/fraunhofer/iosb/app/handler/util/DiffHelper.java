package de.fraunhofer.iosb.app.handler.util;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class DiffHelper {
    public static Map<PolicyBinding, Asset> getToAdd(Map<PolicyBinding, Asset> current, Map<PolicyBinding, Asset> updated) {
        return updated.entrySet().stream()
                .filter(entry -> !current.containsValue(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<PolicyBinding, Asset> getToUpdate(Map<PolicyBinding, Asset> current, Map<PolicyBinding, Asset> updated) {
        return updated.entrySet().stream()
                // Get all that have an ID which already exists
                .filter(entry -> current.entrySet().stream()
                        .anyMatch(existing -> existing.getValue().getId().equals(entry.getValue().getId())))
                // Now filter away equals. What remains are metadata updates.
                .filter(entry -> !current.containsValue(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<PolicyBinding, Asset> getToRemove(Map<PolicyBinding, Asset> current, Map<PolicyBinding, Asset> updated) {
        return current.entrySet().stream()
                // Filter all "to remove"
                .filter(entry -> !updated.containsValue(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
