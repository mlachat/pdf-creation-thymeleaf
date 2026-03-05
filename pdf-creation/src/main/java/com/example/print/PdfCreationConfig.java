package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for PDF generation beans.
 */
@Configuration
class PdfCreationConfig {

    /**
     * Creates a {@link FreemarkerRenderer} bean.
     *
     * @return a new renderer instance
     */
    @Bean
    FreemarkerRenderer freemarkerRenderer() {
        return new FreemarkerRenderer();
    }

    /**
     * Creates a {@link PdfGenerator} bean.
     *
     * @return a new generator instance
     */
    @Bean
    PdfGenerator pdfGenerator() {
        return new PdfGenerator();
    }
}
