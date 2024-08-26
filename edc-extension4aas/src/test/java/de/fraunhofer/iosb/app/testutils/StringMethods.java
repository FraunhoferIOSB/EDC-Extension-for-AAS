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
package de.fraunhofer.iosb.app.testutils;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringMethods {

    public static void assertEqualsIgnoreWhiteSpace(String expected, String actual) {
        assertTrue(expected != null || actual == null);
        assertNotNull(actual);

        assertEquals(expected.replaceAll(" +", "").replaceAll("\r", "").replaceAll("\n",
                ""), actual.replaceAll(" +", "").replaceAll("\r", "").replaceAll("\n", ""));
    }

    public static String resultOf(Object content) throws SerializationException {
        return "{\"result\":[%s]}".formatted(content == null ? "" : new JsonSerializer().write(content));
    }

    public static String resultOfCollection(Collection<?> content) throws SerializationException {
        return "{\"result\":%s}".formatted(new JsonSerializer().write(content));
    }
}
