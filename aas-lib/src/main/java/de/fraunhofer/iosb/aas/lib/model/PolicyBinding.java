package de.fraunhofer.iosb.aas.lib.model;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.policy.model.Policy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Binds an AAS element to access and usage policy.
 * If any of the policies are null, a default policy is to be used.
 */
public record PolicyBinding(Reference referredElement, Policy accessPolicy, Policy usagePolicy) {
    public PolicyBinding(Reference referredElement, Policy accessPolicy, Policy usagePolicy) {
        this.referredElement = Objects.requireNonNull(referredElement);
        this.accessPolicy = accessPolicy;
        this.usagePolicy = usagePolicy;
    }

    @Override
    public @NotNull Reference referredElement() {
        return referredElement;
    }

    @Override
    public @Nullable Policy accessPolicy() {
        return accessPolicy;
    }

    @Override
    public @Nullable Policy usagePolicy() {
        return usagePolicy;
    }
}
