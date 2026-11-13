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
package de.fraunhofer.iosb.ilt.dataspace.app.util;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Base64;


public final class RsaKeyUtils {

    private RsaKeyUtils() {
        // Utility class
    }

    public record PemKeys(
            String privateKeyPkcs8Pem,
            String publicKeyPem) {

        // Avoid exposing private keys through the generated record toString().
        @Override
        public String toString() {
            return "PemKeys[redacted]";
        }
    }

    public static PemKeys generate() throws GeneralSecurityException {
        return generate(2048);
    }


    public static PemKeys generate(int keySize)
            throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(keySize);
        KeyPair pair = generator.generateKeyPair();

        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) pair.getPrivate();

        return new PemKeys(
                toPem("PRIVATE KEY", privateKey.getEncoded()),
                toPem("PUBLIC KEY", pair.getPublic().getEncoded()));
    }


    private static byte[] encodePkcs1(RSAPrivateCrtKey key) {
        BigInteger[] components = {
                BigInteger.ZERO, // Version: two-prime RSA
                key.getModulus(),
                key.getPublicExponent(),
                key.getPrivateExponent(),
                key.getPrimeP(),
                key.getPrimeQ(),
                key.getPrimeExponentP(),
                key.getPrimeExponentQ(),
                key.getCrtCoefficient()
        };

        ByteArrayOutputStream sequence = new ByteArrayOutputStream();
        for (BigInteger component: components) {
            sequence.writeBytes(der(0x02, component.toByteArray()));
        }

        return der(0x30, sequence.toByteArray());
    }


    private static byte[] der(int tag, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);

        int length = value.length;
        if (length < 128) {
            out.write(length);
        }
        else {
            int byteCount = (32 - Integer.numberOfLeadingZeros(length) + 7) / 8;

            out.write(0x80 | byteCount);
            for (int i = byteCount - 1; i >= 0; i--) {
                out.write((length >>> (8 * i)) & 0xFF);
            }
        }

        out.writeBytes(value);
        return out.toByteArray();
    }


    private static String toPem(String label, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(
                64, new byte[] { '\n' }).encodeToString(encoded);

        return "-----BEGIN " + label + "-----\n"
                + base64 + "\n"
                + "-----END " + label + "-----\n";
    }
}
