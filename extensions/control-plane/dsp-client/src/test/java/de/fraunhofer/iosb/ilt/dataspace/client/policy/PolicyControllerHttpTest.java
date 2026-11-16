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
package de.fraunhofer.iosb.ilt.dataspace.client.policy;

import com.fasterxml.jackson.databind.JsonNode;
import de.fraunhofer.iosb.ilt.dataspace.client.testutils.DspClientTestBase;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Layer-2 integration tests for {@link PolicyController}: accepted-policy CRUD over real HTTP against a booted EDC
 * runtime with the real {@link AcceptedPolicyStore} and real JSON (de)serialization.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PolicyControllerHttpTest extends DspClientTestBase {

    private static final String POLICY_TARGET = "-1379397334";
    private static final String USE_ACTION = "USE";

    @RegisterExtension
    static RuntimePerClassExtension runtime = DspClientTestBase.leanRuntime();

    @BeforeAll
    static void awaitJetty() {
        awaitRuntimeReadiness(acceptedPolicyDefinitionsUrl(LEAN_RUNTIME_PORT));
    }


    @Override
    protected int runtimePort() {
        return LEAN_RUNTIME_PORT;
    }


    @Test
    @Order(1)
    void getOnEmptyStoreReturnsEmptyListTest() {
        try (var response = getAcceptedPolicyDefinitions()) {
            assertEquals(200, response.code());
            assertEquals("application/json", contentType(response), "empty store content type");
            assertEquals("[]", body(response).trim(), "empty store must return an empty JSON array");
        }
    }


    @Test
    void postPolicyDefinitionIsReturnedByGetWithRoundTrippedPolicyTest() throws Exception {
        var policyDefinitionId = "test-policy-post";
        var postedJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, POLICY_TARGET, true));

        try (var post = postAcceptedPolicyDefinition(postedJson)) {
            assertEquals(204, post.code(), "POST accepted policy definition");
        }

        try (var get = getAcceptedPolicyDefinitions()) {
            assertEquals(200, get.code());
            var fetched = MAPPER.readTree(body(get));
            var entry = entryWithId(fetched, policyDefinitionId);
            assertNotNull(entry, "posted policy definition must be returned by GET");
            assertEquals(MAPPER.readTree(postedJson), entry, "GET must return the posted PolicyDefinition incl. embedded Policy as JSON");
            assertEquals(POLICY_TARGET, entry.at("/policy/target").asText());
            assertEquals(USE_ACTION, entry.at("/policy/permissions/0/action/type").asText());
        }
    }


    @Test
    void postDuplicatePolicyDefinitionIsIgnoredTest() throws Exception {
        var policyDefinitionId = "test-policy-duplicate";
        var policyJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, POLICY_TARGET, false));

        try (var post = postAcceptedPolicyDefinition(policyJson)) {
            assertEquals(204, post.code());
        }
        try (var duplicatePost = postAcceptedPolicyDefinition(policyJson)) {
            assertEquals(204, duplicatePost.code(), "duplicate POST must not fail");
        }

        try (var get = getAcceptedPolicyDefinitions()) {
            assertEquals(1, countEntriesWithId(MAPPER.readTree(body(get)), policyDefinitionId), "store contains-check ignores duplicates");
        }
    }


    @Test
    void postDuplicateRuleBearingPolicyDefinitionIsStoredTwiceTest() throws Exception {
        var policyDefinitionId = "test-policy-duplicate-with-rules";
        var policyJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, POLICY_TARGET, true));

        try (var post = postAcceptedPolicyDefinition(policyJson)) {
            assertEquals(204, post.code());
        }
        try (var duplicatePost = postAcceptedPolicyDefinition(policyJson)) {
            assertEquals(204, duplicatePost.code());
        }

        try (var get = getAcceptedPolicyDefinitions()) {
            assertEquals(2, countEntriesWithId(MAPPER.readTree(body(get)), policyDefinitionId),
                    "policy-model 0.17.0 rules use identity equality, so the store contains-check keeps both deserialized copies of a rule-bearing policy");
        }
    }


    @Test
    void putUnknownPolicyDefinitionIsCreatedTest() throws Exception {
        var policyDefinitionId = "test-policy-put-create";
        var putJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, POLICY_TARGET, true));

        try (var put = putAcceptedPolicyDefinition(putJson)) {
            assertEquals(201, put.code(), "PUT on unknown id upserts and reports creation");
            assertEquals(apiRoot() + "/" + policyDefinitionId, put.header("Location"),
                    "Jetty absolutizes the raw id set as Location by the controller against the api context root");
            assertEquals("", body(put), "201 carries no entity");
        }

        try (var get = getAcceptedPolicyDefinitions()) {
            assertEquals(1, countEntriesWithId(MAPPER.readTree(body(get)), policyDefinitionId), "upserted policy definition must be stored");
        }
    }


    @Test
    void putKnownPolicyDefinitionIsReplacedTest() throws Exception {
        var policyDefinitionId = "test-policy-put-replace";
        var originalJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, POLICY_TARGET, true));
        var replacementJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, "replaced-target", true));

        try (var post = postAcceptedPolicyDefinition(originalJson)) {
            assertEquals(204, post.code());
        }

        try (var put = putAcceptedPolicyDefinition(replacementJson)) {
            assertEquals(202, put.code(), "PUT on a known id replaces and reports accepted");
            assertEquals(null, put.header("Location"), "202 reply does not carry a Location header");
            assertEquals("\"" + policyDefinitionId + "\"", body(put), "202 entity is the policy definition id URI serialized as JSON string");
            assertEquals("application/json", contentType(put), "URI entity is serialized by the Jackson writer");
        }

        try (var get = getAcceptedPolicyDefinitions()) {
            var fetched = MAPPER.readTree(body(get));
            assertEquals(1, countEntriesWithId(fetched, policyDefinitionId));
            assertEquals("replaced-target", entryWithId(fetched, policyDefinitionId).at("/policy/target").asText(), "GET must return the replaced policy definition");
        }
    }


    @Test
    void deleteExistingPolicyDefinitionRemovesItTest() throws Exception {
        var policyDefinitionId = "test-policy-delete";
        var policyJson = MAPPER.writeValueAsString(policyDefinition(policyDefinitionId, POLICY_TARGET, true));

        try (var post = postAcceptedPolicyDefinition(policyJson)) {
            assertEquals(204, post.code());
        }

        try (var delete = deleteAcceptedPolicyDefinition(policyDefinitionId)) {
            assertEquals(204, delete.code(), "DELETE on existing policy definition");
            assertEquals("", body(delete), "204 carries no entity");
        }

        try (var get = getAcceptedPolicyDefinitions()) {
            assertEquals(0, countEntriesWithId(MAPPER.readTree(body(get)), policyDefinitionId), "deleted policy definition must be absent");
        }
    }


    @Test
    void deleteUnknownPolicyDefinitionReturnsNotFoundTest() throws Exception {
        try (var delete = deleteAcceptedPolicyDefinition("test-policy-never-posted")) {
            assertEquals(404, delete.code(), "DELETE on unknown id maps ObjectNotFoundException");
            assertEquals("application/json", contentType(delete), "404 detail body is ApiErrorDetail JSON");
            var error = MAPPER.readTree(body(delete));
            assertTrue(error.isArray(), "EdcApiExceptionMapper serializes a list of ApiErrorDetail");
            assertTrue(error.get(0).path("message").asText().contains("test-policy-never-posted"), "detail mentions the missing id");
        }
    }


    @Test
    void postEmptyBodyReturnsBadRequestTest() throws Exception {
        try (var post = postAcceptedPolicyDefinition("", "application/json")) {
            assertEquals(400, post.code(), "empty body must not be accepted");
            assertEquals("application/json", contentType(post), "EdcApiExceptionMapper renders the controller InvalidRequestException as ApiErrorDetail JSON");
            var errors = MAPPER.readTree(body(post));
            assertTrue(errors.isArray(), "EdcApiExceptionMapper serializes a list of ApiErrorDetail");
            assertEquals("Missing request body of type policyDefinition", errors.get(0).path("message").asText());
            assertEquals("InvalidRequest", errors.get(0).path("type").asText());
        }
    }


    @Test
    void putEmptyBodyReturnsBadRequestTest() throws Exception {
        try (var put = putAcceptedPolicyDefinition("", "application/json")) {
            assertEquals(400, put.code(), "empty body must not be accepted");
            assertEquals("application/json", contentType(put), "EdcApiExceptionMapper renders the controller InvalidRequestException as ApiErrorDetail JSON");
            var errors = MAPPER.readTree(body(put));
            assertTrue(errors.isArray(), "EdcApiExceptionMapper serializes a list of ApiErrorDetail");
            assertEquals("Missing request body of type PolicyDefinition", errors.get(0).path("message").asText());
            assertEquals("InvalidRequest", errors.get(0).path("type").asText());
        }
    }


    @Test
    void postMalformedBodyReturnsBadRequestTest() {
        try (var post = postAcceptedPolicyDefinition("{\"malformed\":", "application/json")) {
            assertEquals(400, post.code(), "malformed JSON maps to 400 via the Jackson exception mappers");
            assertEquals("text/plain", contentType(post), "Jackson exception mappers reply text/plain");
        }
    }


    @Test
    void putMalformedBodyReturnsBadRequestTest() {
        try (var put = putAcceptedPolicyDefinition("{\"malformed\":", "application/json")) {
            assertEquals(400, put.code(), "malformed JSON maps to 400 via the Jackson exception mappers");
            assertEquals("text/plain", contentType(put), "Jackson exception mappers reply text/plain");
        }
    }


    private static PolicyDefinition policyDefinition(String id, String target, boolean withPermission) {
        var policyBuilder = Policy.Builder.newInstance().target(target);
        if (withPermission) {
            policyBuilder.permission(Permission.Builder.newInstance()
                    .action(Action.Builder.newInstance().type(USE_ACTION).build())
                    .build());
        }
        return PolicyDefinition.Builder.newInstance().id(id).policy(policyBuilder.build()).build();
    }


    private static JsonNode entryWithId(JsonNode fetched, String id) {
        for (var entry: fetched) {
            if (id.equals(entry.path("id").asText())) {
                return entry;
            }
        }
        return null;
    }


    private static int countEntriesWithId(JsonNode fetched, String id) {
        var count = 0;
        for (var entry: fetched) {
            if (id.equals(entry.path("id").asText())) {
                count++;
            }
        }
        return count;
    }
}
