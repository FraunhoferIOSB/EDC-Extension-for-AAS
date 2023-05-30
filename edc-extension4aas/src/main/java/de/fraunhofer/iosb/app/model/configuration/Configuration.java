/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.app.model.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.net.URL;
import java.util.Objects;

/**
 * Singleton class.
 * The configuration of the application.
 */
@JsonSerialize
@JsonAutoDetect
public class Configuration {

    private static final String SETTINGS_PREFIX = "edc.aas.";

    private static Configuration instance;

    @JsonProperty(SETTINGS_PREFIX + "remoteAASLocation")
    private URL remoteAasLocation;

    @JsonProperty(SETTINGS_PREFIX + "localAASModelPath")
    private String localAasModelPath;

    @JsonProperty(SETTINGS_PREFIX + "localAASServicePort")
    private int localAasServicePort;

    @JsonProperty(SETTINGS_PREFIX + "localAASServiceConfigPath")
    private String aasServiceConfigPath;

    @JsonProperty(SETTINGS_PREFIX + "syncPeriod")
    private int syncPeriod = 5; // Seconds

    @JsonProperty(SETTINGS_PREFIX + "exposeSelfDescription")
    private boolean exposeSelfDescription = true;

    @JsonProperty(SETTINGS_PREFIX + "defaultAccessPolicyPath")
    private String defaultAccessPolicyPath;

    @JsonProperty(SETTINGS_PREFIX + "defaultContractPolicyPath")
    private String defaultContractPolicyPath;

    @JsonProperty(SETTINGS_PREFIX + "client.waitForTransferTimeout")
    private int waitForTransferTimeout = 10; // Seconds

    @JsonProperty(SETTINGS_PREFIX + "client.waitForAgreementTimeout")
    private int waitForAgreementTimeout = 10; // Seconds

    @JsonProperty(SETTINGS_PREFIX + "client.waitForCatalogTimeout")
    private int waitForCatalogTimeout = 10; // Seconds

    @JsonProperty(SETTINGS_PREFIX + "client.acceptAllProviderOffers")
    private boolean acceptAllProviderOffers = false;

    @JsonProperty(SETTINGS_PREFIX + "client.acceptedContractOffersPath")
    private String acceptedContractOffersPath;

    public static synchronized Configuration getInstance() {
        if (Objects.isNull(instance)) {
            instance = new Configuration();
        }
        return instance;
    }

    public URL getRemoteAasLocation() {
        return remoteAasLocation;
    }

    public void setRemoteAasLocation(URL remoteAasLocation) {
        this.remoteAasLocation = remoteAasLocation;
    }

    public String getLocalAasModelPath() {
        return localAasModelPath;
    }

    public void setLocalAasModelPath(String localAasModelPath) {
        this.localAasModelPath = localAasModelPath;
    }

    public int getLocalAasServicePort() {
        return localAasServicePort;
    }

    public void setLocalAasServicePort(int localAasServicePort) {
        this.localAasServicePort = localAasServicePort;
    }

    public String getAasServiceConfigPath() {
        return aasServiceConfigPath;
    }

    public void setAasServiceConfigPath(String aasServiceConfigPath) {
        this.aasServiceConfigPath = aasServiceConfigPath;
    }

    public int getSyncPeriod() {
        return syncPeriod;
    }

    public void setSyncPeriod(int syncPeriod) {
        this.syncPeriod = syncPeriod;
    }

    public boolean isExposeSelfDescription() {
        return exposeSelfDescription;
    }

    public void setExposeSelfDescription(boolean exposeSelfDescription) {
        this.exposeSelfDescription = exposeSelfDescription;
    }

    public String getDefaultAccessPolicyPath() {
        return defaultAccessPolicyPath;
    }

    public void setDefaultAccessPolicyPath(String defaultAccessPolicyPath) {
        this.defaultAccessPolicyPath = defaultAccessPolicyPath;
    }

    public String getDefaultContractPolicyPath() {
        return defaultContractPolicyPath;
    }

    public void setDefaultContractPolicyPath(String defaultContractPolicyPath) {
        this.defaultContractPolicyPath = defaultContractPolicyPath;
    }

    public int getWaitForAgreementTimeout() {
        return waitForAgreementTimeout;
    }

    public void setWaitForAgreementTimeout(int waitForAgreementTimeout) {
        this.waitForAgreementTimeout = waitForAgreementTimeout;
    }

    public int getWaitForTransferTimeout() {
        return waitForTransferTimeout;
    }

    public void setWaitForTransferTimeout(int waitForTransferTimeout) {
        this.waitForTransferTimeout = waitForTransferTimeout;
    }

    public int getWaitForCatalogTimeout() {
        return waitForCatalogTimeout;
    }

    public void setWaitForCatalogTimeout(int waitForCatalogTimeout) {
        this.waitForCatalogTimeout = waitForCatalogTimeout;
    }

    public boolean isAcceptAllProviderOffers() {
        return acceptAllProviderOffers;
    }

    public void setAcceptAllProviderOffers(boolean acceptAllProviderOffers) {
        this.acceptAllProviderOffers = acceptAllProviderOffers;
    }

    public String getAcceptedContractOffersPath() {
        return acceptedContractOffersPath;
    }

    public void setAcceptedContractOffersPath(String acceptedContractOffersPath) {
        this.acceptedContractOffersPath = acceptedContractOffersPath;
    }
}
