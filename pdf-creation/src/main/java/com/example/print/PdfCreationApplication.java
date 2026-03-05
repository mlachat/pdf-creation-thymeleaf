package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

/**
 * Spring Boot CLI application that generates a sample PDF document.
 *
 * <p>Renders a FreeMarker template with sample data (including a QR code image)
 * and writes the resulting PDF to {@code output/letter.pdf}.</p>
 */
@SpringBootApplication
public class PdfCreationApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PdfCreationApplication.class);

    private static final String DEFAULT_OUTPUT = "output/letter.pdf";

    /**
     * Application entry point.
     *
     * @param args optional first argument: output file path (defaults to {@code output/letter.pdf})
     */
    public static void main(String[] args) {
        SpringApplication.run(PdfCreationApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        var outputPath = Path.of(args.length > 0 ? args[0] : DEFAULT_OUTPUT);
        Files.createDirectories(outputPath.getParent());

        var qrDataUri = loadQrImageAsDataUri();

        Map<String, Object> model = Map.ofEntries(
                Map.entry("name", "Hans Müller"),
                Map.entry("street", "Königstraße 42"),
                Map.entry("city", "80331 München"),
                Map.entry("heading", "Testbrief"),
                Map.entry("title", "Pipeline Demo"),
                Map.entry("message", "Sehr geehrter Herr Müller, dies ist ein automatisch generierter Testbrief."),
                Map.entry("qrCodeDataUri", qrDataUri),
                Map.entry("senderLine", "ACME GmbH | Innovationsweg 10 | 80339 München")
        );

        var renderer = new FreemarkerRenderer();
        var html = renderer.render("test-freemarker", model);

        var baseUri = getClass().getClassLoader().getResource("").toExternalForm();
        var pdfGenerator = new PdfGenerator();
        var pdfBytes = pdfGenerator.generatePdf(html, baseUri);

        Files.write(outputPath, pdfBytes);
        log.info("PDF written to {} ({} bytes)", outputPath.toAbsolutePath(), pdfBytes.length);
    }

    private String loadQrImageAsDataUri() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("images/qr-sample.png")) {
            if (is == null) {
                throw new IllegalStateException("qr-sample.png not found on classpath");
            }
            var bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load QR image", e);
        }
    }
}
