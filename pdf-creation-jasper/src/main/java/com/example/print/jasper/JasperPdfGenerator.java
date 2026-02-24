package com.example.print.jasper;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Generates PDF documents from a compiled JasperReports template ({@code letter.jrxml}).
 *
 * <p>The constructor compiles the JRXML template from the classpath on each instantiation.</p>
 */
public class JasperPdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(JasperPdfGenerator.class);

    private static final String TEMPLATE_RESOURCE_PATH = "/templates/letter.jrxml";

    private final JasperReport compiledReport;

    public JasperPdfGenerator() {
        try (InputStream jrxml = getClass().getResourceAsStream(TEMPLATE_RESOURCE_PATH)) {
            if (jrxml == null) {
                throw new IllegalStateException(
                        "Template not found on classpath: " + TEMPLATE_RESOURCE_PATH);
            }
            compiledReport = JasperCompileManager.compileReport(jrxml);
        } catch (JRException e) {
            log.error("Failed to compile JasperReport template", e);
            throw new IllegalStateException("Failed to compile JasperReport template", e);
        } catch (java.io.IOException e) {
            log.error("Failed to close template stream", e);
            throw new java.io.UncheckedIOException("Failed to close template stream", e);
        }
    }

    /**
     * Fills the compiled report with the given parameters and exports it as a PDF byte array.
     *
     * @param parameters report parameters; must not be {@code null}
     * @return the generated PDF as a byte array
     * @throws NullPointerException  if {@code parameters} is {@code null}
     * @throws IllegalStateException if report filling or PDF export fails
     */
    public byte[] generatePdf(Map<String, Object> parameters) {
        Objects.requireNonNull(parameters, "parameters must not be null");
        log.debug("Starting JasperReports PDF generation");
        try {
            var print = JasperFillManager.fillReport(
                    compiledReport, parameters, new JREmptyDataSource());
            var pdf = JasperExportManager.exportReportToPdf(print);
            log.debug("JasperReports PDF generation complete ({} bytes)", pdf.length);
            return pdf;
        } catch (JRException e) {
            log.error("Failed to generate PDF", e);
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }
}
