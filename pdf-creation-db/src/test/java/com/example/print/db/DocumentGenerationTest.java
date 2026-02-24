package com.example.print.db;

import com.example.print.db.entity.Address;
import com.example.print.db.entity.Document;
import com.example.print.db.entity.Person;
import com.example.print.db.repository.AddressRepository;
import com.example.print.db.repository.DocumentRepository;
import com.example.print.db.repository.PersonRepository;
import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentGenerationTest {

    private static final int SEED_COUNT = 10_000;
    private static final byte[] PDF_MAGIC_BYTES = "%PDF".getBytes(StandardCharsets.US_ASCII);

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    PersonRepository personRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    DocumentRepository documentRepository;

    private FreemarkerRenderer renderer;
    private PdfGenerator pdfGenerator;
    private String qrDataUri;
    private String baseUri;

    @BeforeAll
    void setUp() throws IOException {
        renderer = new FreemarkerRenderer();
        pdfGenerator = new PdfGenerator();
        qrDataUri = loadQrImageAsDataUri();
        baseUri = getClass().getClassLoader().getResource("").toExternalForm();
    }

    @Test
    void seedDataLoaded() {
        assertThat(personRepository.count()).isEqualTo(SEED_COUNT);
        assertThat(addressRepository.count()).isEqualTo(SEED_COUNT);
    }

    @Test
    void generateSingleDocument() {
        Person person = personRepository.findById(1L).orElseThrow();
        Address address = addressRepository.findByPersonId(1L).orElseThrow();

        byte[] pdfBytes = generatePdf(person, address);

        Document doc = createDocument(person, pdfBytes);
        documentRepository.save(doc);

        assertThat(pdfBytes).startsWith(PDF_MAGIC_BYTES);
        assertThat(pdfBytes.length).isGreaterThan(10 * 1024);
        assertThat(documentRepository.count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Transactional
    void generateDocumentsForAllPersons() {
        List<Address> addresses = addressRepository.findAllWithPerson();
        assertThat(addresses).hasSize(SEED_COUNT);

        List<Document> batch = new ArrayList<>(50);
        for (Address address : addresses) {
            byte[] pdfBytes = generatePdf(address.getPerson(), address);
            batch.add(createDocument(address.getPerson(), pdfBytes));

            if (batch.size() >= 50) {
                documentRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            documentRepository.saveAll(batch);
        }

        assertThat(documentRepository.count()).isGreaterThanOrEqualTo(SEED_COUNT);
    }

    @Test
    void documentHasPersonButNoAddressReference() {
        var fields = Document.class.getDeclaredFields();
        var fieldNames = java.util.Arrays.stream(fields)
                .map(java.lang.reflect.Field::getName)
                .toList();

        assertThat(fieldNames).contains("person");
        assertThat(fieldNames).doesNotContain("address");
    }

    @Test
    void storedPdfIsValid() throws IOException {
        Person person = personRepository.findById(1L).orElseThrow();
        Address address = addressRepository.findByPersonId(1L).orElseThrow();

        byte[] pdfBytes = generatePdf(person, address);
        Document saved = documentRepository.save(createDocument(person, pdfBytes));

        Document loaded = documentRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getPdfData()).startsWith(PDF_MAGIC_BYTES);

        try (var pdfDoc = Loader.loadPDF(loaded.getPdfData())) {
            String text = new PDFTextStripper().getText(pdfDoc);
            assertThat(text).contains(person.getFirstName() + " " + person.getLastName());
        }
    }

    @Test
    @Transactional
    void benchmark10kPdfGeneration() {
        List<Address> addresses = addressRepository.findAllWithPerson();
        assertThat(addresses).hasSize(SEED_COUNT);

        System.out.println("\n=== DB PDF Generation Benchmark: " + SEED_COUNT + " Documents ===\n");

        // Warmup: generate first 100 PDFs without measuring
        int warmupCount = 100;
        for (int i = 0; i < warmupCount; i++) {
            Address address = addresses.get(i);
            generatePdf(address.getPerson(), address);
        }
        System.out.println("  Warmup complete (" + warmupCount + " docs)\n");

        // Timed run: generate all 10k PDFs + save to DB
        long[] perDocNanos = new long[SEED_COUNT];
        long totalPdfBytes = 0;
        List<Document> batch = new ArrayList<>(50);

        long totalStart = System.nanoTime();

        for (int i = 0; i < SEED_COUNT; i++) {
            Address address = addresses.get(i);
            Person person = address.getPerson();

            long docStart = System.nanoTime();
            byte[] pdfBytes = generatePdf(person, address);
            perDocNanos[i] = System.nanoTime() - docStart;

            totalPdfBytes += pdfBytes.length;
            batch.add(createDocument(person, pdfBytes));

            if (batch.size() >= 50) {
                documentRepository.saveAll(batch);
                batch.clear();
            }

            if ((i + 1) % 1000 == 0) {
                System.out.println("  " + (i + 1) + "/" + SEED_COUNT + " done");
            }
        }
        if (!batch.isEmpty()) {
            documentRepository.saveAll(batch);
        }

        long totalNanos = System.nanoTime() - totalStart;

        // Compute steady-state stats (skip warmup)
        long steadyStateTotal = 0;
        for (int i = warmupCount; i < SEED_COUNT; i++) {
            steadyStateTotal += perDocNanos[i];
        }
        int steadyCount = SEED_COUNT - warmupCount;

        // Percentiles
        long[] sorted = perDocNanos.clone();
        java.util.Arrays.sort(sorted);

        double totalSec = totalNanos / 1_000_000_000.0;
        double avgMs = (totalNanos / (double) SEED_COUNT) / 1_000_000.0;
        double steadyAvgMs = (steadyStateTotal / (double) steadyCount) / 1_000_000.0;
        double throughput = SEED_COUNT / totalSec;
        double steadyThroughput = steadyCount / (steadyStateTotal / 1_000_000_000.0);
        double p50 = sorted[(int) (SEED_COUNT * 0.50)] / 1_000_000.0;
        double p95 = sorted[(int) (SEED_COUNT * 0.95)] / 1_000_000.0;
        double p99 = sorted[(int) (SEED_COUNT * 0.99)] / 1_000_000.0;
        double totalMB = totalPdfBytes / (1024.0 * 1024.0);
        double avgKB = (totalPdfBytes / (double) SEED_COUNT) / 1024.0;

        String sep = "=".repeat(60);
        System.out.println("\n" + sep);
        System.out.println("  DB BENCHMARK RESULTS (" + SEED_COUNT + " documents)");
        System.out.println(sep);
        System.out.printf("  %-35s %20s%n", "Total time", String.format("%.1fs", totalSec));
        System.out.printf("  %-35s %20s%n", "Throughput", String.format("%.0f PDFs/s", throughput));
        System.out.printf("  %-35s %20s%n", "Avg time per PDF", String.format("%.2fms", avgMs));
        System.out.printf("  %-35s %20s%n", "Steady-state avg", String.format("%.2fms", steadyAvgMs));
        System.out.printf("  %-35s %20s%n", "Steady-state throughput", String.format("%.0f PDFs/s", steadyThroughput));
        System.out.printf("  %-35s %20s%n", "P50 latency", String.format("%.1fms", p50));
        System.out.printf("  %-35s %20s%n", "P95 latency", String.format("%.1fms", p95));
        System.out.printf("  %-35s %20s%n", "P99 latency", String.format("%.1fms", p99));
        System.out.printf("  %-35s %20s%n", "Total output size", String.format("%.0f MB", totalMB));
        System.out.printf("  %-35s %20s%n", "Avg PDF file size", String.format("%.1f KB", avgKB));
        System.out.printf("  %-35s %20s%n", "Documents in DB", String.format("%d", documentRepository.count()));
        System.out.println(sep + "\n");
    }

    private byte[] generatePdf(Person person, Address address) {
        Map<String, Object> model = Map.ofEntries(
                Map.entry("recipientName", person.getFirstName() + " " + person.getLastName()),
                Map.entry("recipientStreet", address.getStreet()),
                Map.entry("recipientCity", address.getZipCode() + " " + address.getCity()),
                Map.entry("date", "24. Februar 2026"),
                Map.entry("subject", "Betreff: Wichtige Mitteilung"),
                Map.entry("bodyText", "Sehr geehrte Damen und Herren, hiermit möchten wir Sie über wichtige Änderungen informieren."),
                Map.entry("showNotice", true),
                Map.entry("noticeText", "Hinweis: Bitte beachten Sie die beigefügten Unterlagen."),
                Map.entry("qrCodeDataUri", qrDataUri),
                Map.entry("senderName", "Dr. Anna Schmidt"),
                Map.entry("termsText", "Es gelten die Allgemeinen Geschäftsbedingungen der ACME GmbH."),
                Map.entry("showDisclaimer", true),
                Map.entry("disclaimerText", "Haftungsausschluss: Dieses Dokument dient ausschließlich zu Informationszwecken."),
                Map.entry("contactInfo", "Kontakt: info@acme-gmbh.de | Tel: +49 89 123456-0"),
                Map.entry("senderLine", "ACME GmbH | Innovationsweg 10 | 80339 München")
        );

        String html = renderer.render("template-a", model);
        return pdfGenerator.generatePdf(html, baseUri);
    }

    private Document createDocument(Person person, byte[] pdfBytes) {
        Document doc = new Document();
        doc.setPerson(person);
        doc.setFilename(person.getLastName() + "_" + person.getId() + ".pdf");
        doc.setPdfData(pdfBytes);
        doc.setCreatedAt(LocalDateTime.now());
        return doc;
    }

    private String loadQrImageAsDataUri() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("images/qr-sample.png")) {
            assertThat(is).as("qr-sample.png must exist on classpath").isNotNull();
            byte[] bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }
}
