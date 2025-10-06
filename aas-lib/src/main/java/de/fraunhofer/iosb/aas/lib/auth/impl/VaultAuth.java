package de.fraunhofer.iosb.aas.lib.auth.impl;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.spi.security.Vault;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Get vault secret for authentication
 */
public class VaultAuth extends AuthenticationMethod {

    private final Vault vault;
    private final String alias;

    public VaultAuth(Vault vault, String alias) {
        this.vault = vault;
        this.alias = alias;
    }

    @Override
    public Map.Entry<String, String> getHeader() {
        return new AbstractMap.SimpleEntry<>("x-api-key", getValue());
    }

    @Override
    protected String getValue() {
        return vault.resolveSecret(alias);
    }
}
