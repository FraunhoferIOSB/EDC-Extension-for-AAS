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
package de.fraunhofer.iosb.app.aas.ssl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SelfSignedCertificateRetrieverTest {

    private static final String EXPIRED = "https://expired.badssl.com";
    private static final String SELF_SIGNED = "https://self-signed.badssl.com";

    @Test
    void testIsTrusted() throws IOException {
        var url = new URL("https://www.google.com/");
        assertTrue(SelfSignedCertificateRetriever.isTrusted(url));
    }

    @Test
    void testUntrusted() throws IOException {
        var selfSignedUrl = new URL(SELF_SIGNED);
        var expiredUrl = new URL(EXPIRED);

        assertFalse(SelfSignedCertificateRetriever.isTrusted(selfSignedUrl));
        assertFalse(SelfSignedCertificateRetriever.isTrusted(expiredUrl));
    }

}