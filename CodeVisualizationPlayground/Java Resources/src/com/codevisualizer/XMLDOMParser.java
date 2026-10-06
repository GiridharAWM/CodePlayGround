package com.codevisualizer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Standard W3C DOM Parser for visualization.xml concepts.
 * Complies with Section 19 & 20 requirements (no external libs, DOM API only).
 */
public class XMLDOMParser {

    public static class Concept {
        private String name;
        private String category;
        private String title;
        private String description;
        private String sampleCode;

        public Concept(String name, String category, String title, String description, String sampleCode) {
            this.name = name;
            this.category = category;
            this.title = title;
            this.description = description;
            this.sampleCode = sampleCode;
        }

        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getSampleCode() { return sampleCode; }
    }

    /**
     * Parses concepts from an InputStream using W3C DOM.
     */
    public static List<Concept> parseConcepts(InputStream inputStream) {
        List<Concept> concepts = new ArrayList<>();
        if (inputStream == null) {
            return concepts;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Secure XML parsing
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            doc.getDocumentElement().normalize();

            NodeList conceptNodes = doc.getElementsByTagName("concept");
            for (int i = 0; i < conceptNodes.getLength(); i++) {
                Node node = conceptNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String name = element.getAttribute("name");
                    String category = getTagValue("category", element, "General");
                    String title = getTagValue("title", element, capitalize(name));
                    String description = getTagValue("description", element, "");
                    String sampleCode = getTagValue("sampleCode", element, "");

                    concepts.add(new Concept(name, category, title, description, sampleCode));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return concepts;
    }

    /**
     * Parses concepts from a File.
     */
    public static List<Concept> parseConcepts(File file) {
        if (file == null || !file.exists()) {
            return new ArrayList<>();
        }
        try (InputStream is = new java.io.FileInputStream(file)) {
            return parseConcepts(is);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static String getTagValue(String tag, Element element, String defaultValue) {
        NodeList list = element.getElementsByTagName(tag);
        if (list != null && list.getLength() > 0) {
            Node node = list.item(0);
            if (node != null) {
                return node.getTextContent().trim();
            }
        }
        return defaultValue;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
