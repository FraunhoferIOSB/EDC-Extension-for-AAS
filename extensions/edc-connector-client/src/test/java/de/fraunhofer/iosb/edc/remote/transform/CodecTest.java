package de.fraunhofer.iosb.edc.remote.transform;

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
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.fraunhofer.iosb.aas.test.FileManager.loadResource;
import static de.fraunhofer.iosb.aas.test.StringMethods.assertEqualsIgnoreWhiteSpace;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private static PolicyDefinition getPolicyDefinition() {
        return PolicyDefinition.Builder.newInstance()
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
    }

    private static ContractDefinition getContractDefinition() {
        return ContractDefinition.Builder.newInstance()
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
                .build();
    }

    @Test
    void serialize_thenDeserialize_asset() {
        var expected = getAsset();

        String serialized = testSubject.serialize(expected);

        Asset deserialized = testSubject.deserialize(serialized, Asset.class).getContent();

        assertEquals(expected.getId(), deserialized.getId());
        assertEquals(expected.getName(), deserialized.getName());
        assertEquals(expected.getContentType(), deserialized.getContentType());
        assertEquals(expected.getDescription(), deserialized.getDescription());
        assertEquals(expected.getVersion(), deserialized.getVersion());
        assertEquals(expected.getDataAddress(), deserialized.getDataAddress());
        assertEquals(expected.getProperties(), deserialized.getProperties());
        assertEquals(expected.getPrivateProperties(), deserialized.getPrivateProperties());
        // Not covered by JsonObjectToAssetTransformer
        // assertEquals(expected.getCreatedAt(), deserialized.getCreatedAt());
    }

    @Test
    void serialize_thenDeserialize_contractDefinition() {
        ContractDefinition expected = getContractDefinition();

        String serialized = testSubject.serialize(expected);

        ContractDefinition deserialized = testSubject.deserialize(serialized, ContractDefinition.class).getContent();

        assertEquals(expected, deserialized);
    }

    @Test
    void serialize_thenDeserialize_policyDefinition() {
        var expected = getPolicyDefinition();

        String serialized = testSubject.serialize(expected);

        // Serializing the policyDefinition gives the policy an @id which is random.
        // Instead of comparing the json string, we deserialize again and compare with the original object.
        PolicyDefinition deserialized = testSubject.deserialize(serialized, PolicyDefinition.class).getContent();

        assertEquals(expected.getId(), deserialized.getId());
        assertEquals(expected.getPolicy().getPermissions(), deserialized.getPolicy().getPermissions());
        assertEquals(expected.getPolicy().getProhibitions(), deserialized.getPolicy().getProhibitions());
        assertEquals(expected.getPolicy().getObligations(), deserialized.getPolicy().getObligations());
        assertEquals(expected.getPolicy().getAssignee(), deserialized.getPolicy().getAssignee());
        assertEquals(expected.getPolicy().getAssigner(), deserialized.getPolicy().getAssigner());
        assertEquals(expected.getPolicy().getExtensibleProperties(), deserialized.getPolicy().getExtensibleProperties());
        // NOTE inheritsFrom is not serialized by EDC JsonObjectFromPolicyTransformer, so not intended
        // assertEquals(policyDefinition.getPolicy().getInheritsFrom(), deserialized.getPolicy().getInheritsFrom());
        assertEquals(expected.getPolicy().getProfiles(), deserialized.getPolicy().getProfiles());
        assertEquals(expected.getPrivateProperties(), deserialized.getPrivateProperties());
    }

    @Test
    void serialize_thenDeserialize_querySpec() {
        QuerySpec querySpec = QuerySpec.Builder.newInstance()
                .range(new Range(0, 31415926)).limit(761370123)
                .offset(42)
                .filter(Criterion.criterion("aas:id", "=", "my-test-aas-id"))
                .build();

        String serialized = testSubject.serialize(querySpec);

        var toMatch = loadResource("querySpec.json");

        assertEqualsIgnoreWhiteSpace(toMatch, serialized);
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