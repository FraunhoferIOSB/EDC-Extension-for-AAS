package de.fraunhofer.iosb.app.model.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.URL;
import java.util.Objects;

/**
 * Singleton class.
 * The configuration of the application.
 */
@JsonSerialize
public class Configuration {

    private static final String SETTINGS_PREFIX = "edc.idsaasapp.";

    private static Configuration instance;

    private boolean initialized;
    private URL remoteAasLocation;
    private String localAasModelPath;
    private int localAasServicePort;
    private String logPrefix = "IDS AAS Extension";
    private String aasServiceConfigPath;
    private URL registryUrl;
    private int syncPeriod = 5; // default value: 5 seconds
    private boolean exposeSelfDescription = true;
    private String defaultAccessPolicyDefinitionPath;
    private String defaultContractPolicyDefinitionPath;

    public static synchronized Configuration getInstance() {
        if (Objects.isNull(instance)) {
            instance = new Configuration();
        }
        return instance;
    }

    public URL getRemoteAasLocation() {
        return remoteAasLocation;
    }

    @JsonProperty(SETTINGS_PREFIX + "remoteaaslocation")
    public void setRemoteAasLocation(URL remoteAasLocation) {
        this.remoteAasLocation = remoteAasLocation;
    }

    public String getLocalAasModelPath() {
        return localAasModelPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "localaasmodelpath")
    public void setLocalAasModelPath(String localAasModelPath) {
        this.localAasModelPath = localAasModelPath;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @JsonProperty(SETTINGS_PREFIX + "initialized")
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    @JsonProperty(SETTINGS_PREFIX + "logprefix")
    public void setLogPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public int getLocalAasServicePort() {
        return localAasServicePort;
    }

    @JsonProperty(SETTINGS_PREFIX + "localaasserviceport")
    public void setLocalAasServicePort(int localAasServicePort) {
        this.localAasServicePort = localAasServicePort;
    }

    public String getAasServiceConfigPath() {
        return aasServiceConfigPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "localaasserviceconfigpath")
    public void setAasServiceConfigPath(String aasServiceConfigPath) {
        this.aasServiceConfigPath = aasServiceConfigPath;
    }

    public URL getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(URL registryUrl) {
        this.registryUrl = registryUrl;
    }

    public int getSyncPeriod() {
        return syncPeriod;
    }

    @JsonProperty(SETTINGS_PREFIX + "syncperiod")
    public void setSyncPeriod(int syncPeriod) {
        this.syncPeriod = syncPeriod;
    }

    public boolean isExposeSelfDescription() {
        return exposeSelfDescription;
    }

    @JsonProperty(SETTINGS_PREFIX + "exposeselfdescription")
    public void setExposeSelfDescription(boolean exposeSelfDescription) {
        this.exposeSelfDescription = exposeSelfDescription;
    }

    public String getDefaultAccessPolicyDefinitionPath() {
        return defaultAccessPolicyDefinitionPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "defaultaccesspolicydefinitionpath")
    public void setDefaultAccessPolicyDefinitionPath(String defaultAccessPolicyDefinitionPath) {
        this.defaultAccessPolicyDefinitionPath = defaultAccessPolicyDefinitionPath;
    }

    public String getDefaultContractPolicyDefinitionPath() {
        return defaultContractPolicyDefinitionPath;
    }

    @JsonProperty(SETTINGS_PREFIX + "defaultcontractpolicydefinitionpath")
    public void setDefaultContractPolicyDefinitionPath(String defaultContractPolicyDefinitionPath) {
        this.defaultContractPolicyDefinitionPath = defaultContractPolicyDefinitionPath;
    }
}
