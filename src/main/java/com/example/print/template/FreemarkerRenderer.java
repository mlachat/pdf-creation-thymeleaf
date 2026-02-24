package com.example.print.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class FreemarkerRenderer {

    private final Configuration cfg;

    public FreemarkerRenderer() {
        cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
    }

    public String render(String templateName, Map<String, Object> variables) {
        try {
            Template template = cfg.getTemplate(templateName + ".html");
            StringWriter writer = new StringWriter();
            template.process(variables, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new RuntimeException("Failed to render template: " + templateName, e);
        }
    }
}
