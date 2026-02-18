package com.example.print.pdf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PdfGeneratorTest {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    void generatePdf_hardcodedHtml_returnsValidPdf() {
        String html = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><meta charset="UTF-8"/><title>Test</title></head>
                <body style="font-family: 'DejaVuSans', sans-serif;">
                    <p>Name: Max Müller</p>
                    <p>Address: Königstraße 42</p>
                </body>
                </html>
                """;

        PdfGenerator generator = new PdfGenerator();
        byte[] pdf = generator.generatePdf(html, null);

        assertNotNull(pdf, "PDF byte array must not be null");
        assertTrue(pdf.length > 0, "PDF byte array must not be empty");

        String header = new String(pdf, 0, Math.min(5, pdf.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "PDF must start with %PDF magic bytes");
    }

    @Test
    void generatePdf_templateFile_createsValidPdfFile() throws IOException {
        String baseUri = getClass().getClassLoader().getResource("templates/").toExternalForm();

        InputStream is = getClass().getClassLoader().getResourceAsStream("templates/test-minimal.html");
        assertNotNull(is, "test-minimal.html must exist on classpath");
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        PdfGenerator generator = new PdfGenerator();
        byte[] pdf = generator.generatePdf(html, baseUri);

        Path outputFile = OUTPUT_DIR.resolve("test-minimal.pdf");
        Files.write(outputFile, pdf);

        assertTrue(Files.exists(outputFile), "test-minimal.pdf must exist");
        assertTrue(Files.size(outputFile) > 1024, "test-minimal.pdf must be > 1KB");

        String header = new String(pdf, 0, Math.min(5, pdf.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "PDF must start with %PDF magic bytes");
    }
}
