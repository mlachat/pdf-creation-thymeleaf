package com.example.print.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

class PdfGeneratorTest {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");
    private static final byte[] PDF_MAGIC_BYTES = "%PDF".getBytes(StandardCharsets.US_ASCII);

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    @DisplayName("Hardcoded HTML produces a valid PDF with correct magic bytes")
    void generatePdf_hardcodedHtml_returnsValidPdf() {
        var html = """
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

        var generator = new PdfGenerator();
        var pdf = generator.generatePdf(html, null);

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(pdf).startsWith(PDF_MAGIC_BYTES);
    }

    @Test
    @DisplayName("Template file produces a valid PDF written to disk")
    void generatePdf_templateFile_createsValidPdfFile() throws IOException {
        var baseUri = getClass().getClassLoader().getResource("templates/").toExternalForm();

        try (var is = getClass().getClassLoader().getResourceAsStream("templates/test-minimal.html")) {
            assertThat(is).as("test-minimal.html must exist on classpath").isNotNull();
            var html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            var generator = new PdfGenerator();
            var pdf = generator.generatePdf(html, baseUri);

            var outputFile = OUTPUT_DIR.resolve("test-minimal.pdf");
            Files.write(outputFile, pdf);

            assertThat(outputFile).exists();
            assertThat(Files.size(outputFile)).isGreaterThan(1024);
            assertThat(pdf).startsWith(PDF_MAGIC_BYTES);
        }
    }

    @Test
    @DisplayName("DejaVuSans font is embedded in generated PDF")
    void generatePdf_fontIsEmbedded() throws IOException {
        var html = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><meta charset="UTF-8"/><title>Font Test</title></head>
                <body style="font-family: 'DejaVuSans', sans-serif;">
                    <p>Müller Königstraße Überprüfung</p>
                </body>
                </html>
                """;

        var generator = new PdfGenerator();
        var pdf = generator.generatePdf(html, null);

        try (var document = Loader.loadPDF(pdf)) {
            var page = document.getPage(0);
            var fonts = page.getResources().getFontNames();

            var hasEmbeddedDejaVu = false;
            for (var fontName : fonts) {
                PDFont font = page.getResources().getFont(fontName);
                PDFontDescriptor descriptor = font.getFontDescriptor();
                if (descriptor != null && descriptor.getFontName().contains("DejaVuSans")) {
                    hasEmbeddedDejaVu = true;
                    assertThat(descriptor.getFontFile2())
                            .as("DejaVuSans font stream (FontFile2) must be present — font must be embedded")
                            .isNotNull();
                }
            }
            assertThat(hasEmbeddedDejaVu)
                    .as("PDF must contain an embedded DejaVuSans font")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Null HTML input throws NullPointerException")
    void generatePdf_nullHtml_throwsNpe() {
        var generator = new PdfGenerator();
        assertThatThrownBy(() -> generator.generatePdf(null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
