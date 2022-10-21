package de.fraunhofer.iosb.app.util;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class EncoderTest {

    @Test
    public void testEncodeNull() {
        try {
            Encoder.encodeBase64(null);
            fail("NullPointerException should have been thrown");
        } catch (NullPointerException expected) {
        }
    }
}
