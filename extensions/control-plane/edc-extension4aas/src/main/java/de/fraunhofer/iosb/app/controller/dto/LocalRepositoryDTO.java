package de.fraunhofer.iosb.app.controller.dto;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.model.config.impl.faaast.FaaastRepositoryConfig;

import java.util.List;
import java.util.Objects;


public record LocalRepositoryDTO(String modelPath, Integer port, String configPath, List<PolicyBinding> policyBindings) {
    public LocalRepositoryDTO {
        policyBindings = Objects.requireNonNullElse(policyBindings, List.of());
    }


    public FaaastRepositoryConfig asConfig() {
        return FaaastRepositoryConfig.Builder.newInstance()
                .model(modelPath())
                .port(port())
                .configPath(configPath())
                .policyBindings(policyBindings())
                .build();
    }
}
