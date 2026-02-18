package com.example.print.qr;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeGeneratorTest {

    @Test
    void generateDataUri_returnsValidDataUriPrefix() {
        String result = QrCodeGenerator.generateDataUri("https://example.com/doc/12345", 300);
        assertTrue(result.startsWith("data:image/png;base64,"),
                "Data URI must start with 'data:image/png;base64,'");
    }

    @Test
    void generateDataUri_base64DecodesToValidPng() {
        String result = QrCodeGenerator.generateDataUri("https://example.com/doc/12345", 300);
        String base64Part = result.substring("data:image/png;base64,".length());

        byte[] decoded = Base64.getDecoder().decode(base64Part);
        assertTrue(decoded.length > 0, "Decoded bytes must not be empty");

        // PNG signature: 137 80 78 71 13 10 26 10
        byte[] pngSignature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        byte[] header = new byte[8];
        System.arraycopy(decoded, 0, header, 0, 8);
        assertArrayEquals(pngSignature, header, "Decoded bytes must have PNG signature");
    }
}
