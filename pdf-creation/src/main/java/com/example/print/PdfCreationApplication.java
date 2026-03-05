package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
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

    private final FreemarkerRenderer freemarkerRenderer;
    private final PdfGenerator pdfGenerator;

    /**
     * Constructs the application with required dependencies.
     *
     * @param freemarkerRenderer the template renderer; must not be {@code null}
     * @param pdfGenerator       the PDF generator; must not be {@code null}
     */
    public PdfCreationApplication(FreemarkerRenderer freemarkerRenderer, PdfGenerator pdfGenerator) {
        this.freemarkerRenderer = freemarkerRenderer;
        this.pdfGenerator = pdfGenerator;
    }

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

        var html = freemarkerRenderer.render("test-freemarker", model);

        var baseUri = new ClassPathResource("/").getURL().toExternalForm();
        var pdfBytes = pdfGenerator.generatePdf(html, baseUri);

        Files.write(outputPath, pdfBytes);
        log.info("PDF written to {} ({} bytes)", outputPath.toAbsolutePath(), pdfBytes.length);
    }

    private String loadQrImageAsDataUri() {
        var resource = new ClassPathResource("images/qr-sample.png");
        try (var is = resource.getInputStream()) {
            var bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load QR image", e);
        }
    }
}
