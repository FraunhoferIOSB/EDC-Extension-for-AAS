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
package de.fraunhofer.iosb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class EncoderTest {

    @Test
    public void testEncodeNull() {
        try {
            Encoder.encodeBase64(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testEncodeAsciiString() {
        // Might as well test all ASCII
        assertEquals("ICEiIyQlJlwnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4" +
                        "/QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fg==",
                Encoder.encodeBase64(" !\"#$%&\\'()*+,-./0123456789:;" +
                        "<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    }
}