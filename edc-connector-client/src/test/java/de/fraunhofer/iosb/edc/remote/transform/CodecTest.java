package de.fraunhofer.iosb.edc.remote.transform;

import org.apache.commons.io.FileUtils;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.policy.model.OdrlNamespace;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.message.Range;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test if codec correctly (de)serializes and compacts/expands json-ld contexts
 */
@ExtendWith(DependencyInjectionExtension.class)
class CodecTest {

    private static final String AAS_NAMESPACE = "https://admin-shell.io/aas/3/0/";
    private static final String AAS_PREFIX = "aas";

    private static final EmbeddedRuntime RUNTIME = new EmbeddedRuntime(
            "asset-spi",
            "http-spi",
            "policy-spi",
            "contract-spi",
            "transform-lib",
            "control-plane-transform",
            "json-ld",
            "runtime-core",
            "connector-core",
            "edc-connector-client"
    ).configurationProvider(() -> ConfigFactory.fromMap(Map.of(
            "edc.controlplane.management.url", "http://localhost:1"
    )));

    @RegisterExtension
    static RuntimePerClassExtension runtime = new RuntimePerClassExtension(RUNTIME);
    private static Codec testSubject;

    @BeforeAll
    static void setUp() {
        TypeTransformerRegistry typeTransformerRegistry = runtime.getService(TypeTransformerRegistry.class);
        JsonLd jsonLd = runtime.getService(JsonLd.class);
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE);
        jsonLd.registerNamespace(AAS_PREFIX, AAS_NAMESPACE);
        jsonLd.registerNamespace(OdrlNamespace.ODRL_PREFIX, OdrlNamespace.ODRL_SCHEMA);
        testSubject = new Codec(typeTransformerRegistry, jsonLd);
    }

    public static void assertEqualsIgnoreWhiteSpace(String expected, String actual) {
        assertTrue(expected != null || actual == null);
        assertNotNull(actual);

        assertEquals(expected.replaceAll(" +", "").replaceAll("\r", "").replaceAll("\n",
                ""), actual.replaceAll(" +", "").replaceAll("\r", "").replaceAll("\n", ""));
    }

    @Test
    void deserializeAssets() throws IOException {
        var serialized = readFile("assetSerialized.json");
        var toMatch = getAsset();
        var serializedList = String.format("[%s,%s]", serialized, serialized);

        var deserializedList = testSubject.deserializeList(serializedList, Asset.class);
        deserializedList.getContent().forEach(asset -> compareAssets(toMatch, asset));
    }

    @Test
    void deserializeAsset() throws IOException {
        var serialized = readFile("assetSerialized.json");

        var deserialized = testSubject.deserialize(serialized, Asset.class);

        var toMatch = getAsset();
        compareAssets(toMatch, deserialized.getContent());
    }

    private void compareAssets(Asset assetA, Asset assetB) {
        assertEquals(assetA.getId(), assetB.getId());
        assertEquals(assetA.getContentType(), assetB.getContentType());
        assertEquals(assetA.getDescription(), assetB.getDescription());
        assertEquals(assetA.getDataAddress(), assetB.getDataAddress());

        assertTrue(assetA.getProperties()
                .entrySet().stream()
                .allMatch(entry -> assetB.getProperty(entry.getKey()).equals(entry.getValue())));

        assertTrue(assetA.getPrivateProperties()
                .entrySet().stream()
                .allMatch(entry -> assetB.getPrivateProperty(entry.getKey()).equals(entry.getValue())));
    }

    @Test
    void serializeAsset() throws IOException {
        var asset = getAsset();

        assertEqualsIgnoreWhiteSpace(readFile("assetSerialized.json"), testSubject.serialize(asset));
    }

    @Test
    void serializeContractDefinition() throws IOException {
        var serializationResult = testSubject.serialize(ContractDefinition.Builder.newInstance()
                .id("my-test-contract-definition-id")
                .contractPolicyId("my-test-contract-definition-contract-policy-id")
                .accessPolicyId("my-test-contract-definition-access-policy-id")
                .assetsSelector(List.of(
                        Criterion.Builder.newInstance().operandLeft("my-test-custom-string-operand-left")
                                .operator("?custom-operator\\")
                                .operandRight(new ArrayList<>()).build(),
                        Criterion.criterion("left", "operand", "right")
                ))
                .privateProperty("https://admin-shell.io/aas/3/0/test-private-property", "test-private-property-value")
                .privateProperty("https://admin-shell.io/aas/3/0/test-private-property-2", "test-private-property-2-value")
                .build());

        var toMatch = readFile("contractDefinitionSerialized.json");

        assertEqualsIgnoreWhiteSpace(toMatch, serializationResult);
    }

    @Test
    void serializePolicyDefinition() throws IOException {
        var policyDefinition = PolicyDefinition.Builder.newInstance()
                .id("my-test-contract-definition-id")
                .policy(Policy.Builder.newInstance()
                        .target("my-policy-definition-policy-target")
                        .assignee("my-policy-definition-policy-assignee")
                        .assigner("my-policy-definition-policy-assigner")
                        .inheritsFrom("my-policy-definition-policy-does-not-inherit")
                        .build()
                )
                .privateProperty("https://admin-shell.io/aas/3/0/test-private-property", "test-private-property-value")
                .privateProperty("https://admin-shell.io/aas/3/0/test-private-property-2", "test-private-property-2-value")
                .build();

        var serializationResult = testSubject.serialize(policyDefinition);

        // Serializing the policyDefinition gives the policy an @id which is random.
        // Instead of comparing the json string, we deserialize again and compare with the original object.

        assertEquals(policyDefinition, testSubject.deserializePolicyDefinition(serializationResult));

        var toMatch = readFile("policyDefinitionSerialized.json");

        assertEqualsIgnoreWhiteSpace(toMatch, serializationResult);
    }

    @Test
    void deserializePolicyDefinitions() {
    }

    @Test
    void deserializePolicyDefinition() {
    }

    @Test
    void serializeQuerySpec() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance()
                .range(new Range(0, 31415926)).limit(761370123)
                .offset(42)
                .filter(Criterion.criterion("aas:id", "=", "my-test-aas-id"))
                .build();

        var y = testSubject.serialize(querySpec);

        var z = 3;
    }

    @Test
    void deserializeContractDefinitions() {
    }

    @Test
    void deserializeContractDefinition() {
    }

    private String readFile(String path) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        return FileUtils.readFileToString(new File(Objects.requireNonNull(classLoader.getResource(path)).getFile()),
                StandardCharsets.UTF_8.toString());
    }

    private Asset getAsset() {
        return Asset.Builder.newInstance()
                .id("my-test-asset-id")
                .contentType("test-content-type")
                .description("test-description")
                .property("https://admin-shell.io/aas/3/0/test-property", "test-property-value")
                .property("https://admin-shell.io/aas/3/0/test-property-2", "test-property-2-value")
                .privateProperty("https://admin-shell.io/aas/3/0/test-private-property", "test-private-property-value")
                .privateProperty("https://admin-shell.io/aas/3/0/test-private-property-2", "test-private-property-2-value")
                .build();
    }

}