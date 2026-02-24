package com.example.print.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

/**
 * Renders FreeMarker templates from the classpath {@code templates/} directory.
 */
public class FreemarkerRenderer {

    private static final Logger log = LoggerFactory.getLogger(FreemarkerRenderer.class);

    private static final String TEMPLATE_BASE_DIR = "templates";
    private static final String TEMPLATE_SUFFIX = ".html";

    private final Configuration cfg;

    public FreemarkerRenderer() {
        cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), TEMPLATE_BASE_DIR);
        cfg.setDefaultEncoding("UTF-8");
    }

    /**
     * Renders the named template with the given variable map.
     *
     * @param templateName template name without the {@code .html} suffix; must not be {@code null}
     * @param variables    model variables passed to the template; must not be {@code null}
     * @return the rendered HTML string
     * @throws NullPointerException  if {@code templateName} or {@code variables} is {@code null}
     * @throws IllegalStateException if the template cannot be found or rendered
     */
    public String render(String templateName, Map<String, Object> variables) {
        Objects.requireNonNull(templateName, "templateName must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        log.debug("Rendering template '{}'", templateName);
        try {
            var template = cfg.getTemplate(templateName + TEMPLATE_SUFFIX);
            var writer = new StringWriter();
            template.process(variables, writer);
            log.debug("Template '{}' rendered successfully", templateName);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            log.error("Failed to render template: {}", templateName, e);
            throw new IllegalStateException("Failed to render template: " + templateName, e);
        }
    }
}
