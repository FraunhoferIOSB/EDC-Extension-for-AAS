package de.fraunhofer.iosb.app.aas;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.util.Encoder;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Do not test for empty Shell, Submodel, ConceptDescription inside an environment since the default implementation
 * of the environment class does not allow null values.
 */
class EnvironmentToAssetMapperTest {

    public static final String CONCEPT_DESCRIPTIONS = "conceptDescriptions";
    public static final String SHELLS = "shells";
    public static final String SUBMODELS = "submodels";
    private EnvironmentToAssetMapper testSubject;

    // Change for test case if needed
    private boolean onlySubmodelsDecider;
    private final String accessUrl = "http://localhost:1234";
    private final List<Object> emptyList = List.of();

    @BeforeEach
    void setUp() {
        onlySubmodelsDecider = false;
        testSubject = new EnvironmentToAssetMapper(() -> this.onlySubmodelsDecider);
    }

    // TODO test apply but only part of the environments are correct.
    // TODO Then filtered version and failure messages should be correct.

    @Test
    void testApply() {
        var env = createEnvironment();
        var result = testSubject.apply(Map.of(accessUrl, env));
        assertTrue(result.succeeded());
        assertNotNull(result.getContent());
        var envAsset = result.getContent().get(accessUrl);

        assertNotNull(envAsset);

        // This is essentially the same as in testWholeEnvironmentIdEquality

        assertEquals(env.getSubmodels().stream()
                        .map(Submodel::getId)
                        .map(String::hashCode)
                        .map(String::valueOf)
                        .toList(),
                getChildren(envAsset, SUBMODELS).stream()
                        .map(Asset::getId)
                        .toList());

        assertEquals(env.getAssetAdministrationShells().stream()
                        .map(AssetAdministrationShell::getId)
                        .map(String::hashCode)
                        .map(String::valueOf)
                        .toList(),
                getChildren(envAsset, SHELLS).stream()
                        .map(Asset::getId)
                        .toList());

        assertEquals(env.getConceptDescriptions().stream()
                        .map(ConceptDescription::getId)
                        .map(String::hashCode)
                        .map(String::valueOf)
                        .toList(),
                getChildren(envAsset, CONCEPT_DESCRIPTIONS).stream()
                        .map(Asset::getId)
                        .toList());
    }

    @Test
    void testNullAccessUrl() {
        // This can happen since map implementations can have null keys
        var res = testSubject.executeSingle(null, new DefaultEnvironment());
        assertTrue(res.failed());
    }

    @Test
    void testNullEnvironment() {
        var res = testSubject.executeSingle(accessUrl, null);
        assertTrue(res.failed());
    }

    @Test
    void testNullArgs() {
        var res = testSubject.executeSingle(null, null);
        assertTrue(res.failed());
    }

    @Test
    void testEmptyEnvironment() {
        var result = testSubject.executeSingle(accessUrl, new DefaultEnvironment()).getContent();

        assertEquals(emptyList, result.getValue().getProperty(SUBMODELS));
        assertEquals(emptyList, result.getValue().getProperty(SHELLS));
        assertEquals(emptyList, result.getValue().getProperty(CONCEPT_DESCRIPTIONS));
    }

    @Test
    void testOnlySubmodels() {
        onlySubmodelsDecider = true;

        var env = createEnvironment();

        var result = testSubject.executeSingle(accessUrl, env).getContent();

        assertEquals(env.getSubmodels().stream().map(Submodel::getId).map(String::hashCode).map(String::valueOf).toList(),
                getChildren(result.getValue(), SUBMODELS).stream().map(Asset::getId).toList());
        assertEquals(emptyList, result.getValue().getProperty(SHELLS));
        assertEquals(emptyList, result.getValue().getProperty(CONCEPT_DESCRIPTIONS));
    }

    @Test
    void testWholeEnvironmentIdEquality() {
        var env = createEnvironment();

        var result = testSubject.executeSingle(accessUrl, env);

        assertTrue(result.succeeded());

        var res = result.getContent().getValue();

        assertEquals(env.getSubmodels().stream()
                        .map(Submodel::getId)
                        .map(String::hashCode)
                        .map(String::valueOf)
                        .toList(),
                getChildren(res, SUBMODELS).stream()
                        .map(Asset::getId)
                        .toList());

        assertEquals(env.getAssetAdministrationShells().stream()
                        .map(AssetAdministrationShell::getId)
                        .map(String::hashCode)
                        .map(String::valueOf)
                        .toList(),
                getChildren(res, SHELLS).stream()
                        .map(Asset::getId)
                        .toList());

        assertEquals(env.getConceptDescriptions().stream()
                        .map(ConceptDescription::getId)
                        .map(String::hashCode)
                        .map(String::valueOf)
                        .toList(),
                getChildren(res, CONCEPT_DESCRIPTIONS).stream()
                        .map(Asset::getId)
                        .toList());
    }

    @Test
    void testCorrectAccessUrls() {
        var env = createEnvironment();
        var result = testSubject.executeSingle(accessUrl, env).getContent();
        var shellDataAddress =
                (AasDataAddress) getChildren(result.getValue(), SHELLS).stream().map(Asset::getDataAddress).toList().get(0);
        var submodelDataAddress =
                (AasDataAddress) getChildren(result.getValue(), SUBMODELS).stream().map(Asset::getDataAddress).toList().get(0);
        var conceptDescriptionDataAddress =
                (AasDataAddress) getChildren(result.getValue(), CONCEPT_DESCRIPTIONS).stream().map(Asset::getDataAddress).toList().get(0);

        assertEquals(accessUrl, shellDataAddress.getBaseUrl());
        assertEquals("%s/%s".formatted(SHELLS, Encoder.encodeBase64(env.getAssetAdministrationShells().get(0).getId())),
                shellDataAddress.referenceChainAsPath());
        assertEquals(accessUrl, submodelDataAddress.getBaseUrl());
        assertEquals("%s/%s".formatted(SUBMODELS, Encoder.encodeBase64(env.getSubmodels().get(0).getId())),
                submodelDataAddress.referenceChainAsPath());
        assertEquals(accessUrl, conceptDescriptionDataAddress.getBaseUrl());
        assertEquals("concept-descriptions/%s".formatted(Encoder.encodeBase64(env.getConceptDescriptions().get(0).getId())),
                conceptDescriptionDataAddress.referenceChainAsPath());
    }

    @SuppressWarnings("unchecked")
    private List<Asset> getChildren(Asset parent, String name) {
        return (List<Asset>) parent.getProperty(name);
    }

    private Environment createEnvironment() {
        return new DefaultEnvironment.Builder()
                .submodels(
                        new DefaultSubmodel.Builder()
                                .id(UUID.randomUUID().toString()).build())
                .assetAdministrationShells(
                        new DefaultAssetAdministrationShell.Builder()
                                .id(UUID.randomUUID().toString()).build())
                .conceptDescriptions(
                        new DefaultConceptDescription.Builder().id(
                                UUID.randomUUID().toString()).build())
                .build();
    }
}