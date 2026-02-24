package com.example.print;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Shared test utilities for PDF-generation integration tests.
 */
public final class TestHelper {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");
    private static final byte[] PDF_MAGIC_BYTES = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private TestHelper() {
    }

    /**
     * Loads {@code images/qr-sample.png} from the classpath and returns it as a data-URI string.
     */
    public static String loadQrImageAsDataUri() throws IOException {
        try (var is = TestHelper.class.getClassLoader()
                .getResourceAsStream("images/qr-sample.png")) {
            assertThat(is).as("qr-sample.png must exist on classpath").isNotNull();
            var bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }

    /**
     * Asserts that the given byte array is a valid PDF with at least {@code minSizeKb} kilobytes.
     */
    public static void assertValidPdf(byte[] pdfBytes, int minSizeKb) {
        assertThat(pdfBytes).as("PDF byte array must not be null").isNotNull();
        assertThat(pdfBytes).as("PDF byte array must not be empty").isNotEmpty();
        assertThat(pdfBytes).as("PDF must start with %%PDF magic bytes").startsWith(PDF_MAGIC_BYTES);
        assertThat(pdfBytes.length).as("PDF must be > %dKB, actual: %d", minSizeKb, pdfBytes.length)
                .isGreaterThan(minSizeKb * 1024);
    }

    /**
     * Extracts all text from a PDF byte array using PDFBox.
     *
     * @param pdfBytes the raw PDF bytes
     * @return the extracted text
     * @throws IOException if the PDF cannot be parsed
     */
    public static String extractPdfText(byte[] pdfBytes) throws IOException {
        try (var document = Loader.loadPDF(pdfBytes)) {
            var stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Returns the external-form URI of the test classpath root, suitable as a base URI
     * for resolving CSS and images in rendered HTML.
     *
     * @param testClass the test class whose classloader is used
     * @return the base URI string
     */
    public static String testClasspathBaseUri(Class<?> testClass) {
        return testClass.getClassLoader().getResource("").toExternalForm();
    }

    /**
     * Ensures the shared output directory exists and returns it.
     */
    public static Path ensureOutputDir() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        return OUTPUT_DIR;
    }

    /**
     * Returns a {@link Path} inside the shared output directory for the given filename.
     */
    public static Path outputFile(String filename) {
        return OUTPUT_DIR.resolve(filename);
    }
}
