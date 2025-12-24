/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.parser;

import graphql.util.Constants;
import java.util.regex.Pattern;

/**
 *
 * @author iurif
 */
public class ExternalAttribute {

    public static final String EXTERNAL_REFERENCE_PREFIX = "*";
    public final String className;
    public final String attributeName;

    public ExternalAttribute(String className, String attributeName) {
        this.className = className;
        this.attributeName = attributeName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExternalAttribute)) {
            return false;
        }
        ExternalAttribute other = (ExternalAttribute) obj;
        return className.equals(other.className) && attributeName.equals(other.attributeName);
    }

    @Override
    public int hashCode() {
        return (className + "|" + attributeName).hashCode();
    }

    public static ExternalAttribute parseExternalAttribute(String header) {
        String cleaned = header.substring(EXTERNAL_REFERENCE_PREFIX.length());
        String[] parts = cleaned.split(Pattern.quote(Constants.DEPENDENT_SEPARATOR));
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid external attribute format: " + header);
        }
        return new ExternalAttribute(parts[0], parts[1]);
    }

    public static boolean isExternalAttribute(String header) {
        return header.startsWith(EXTERNAL_REFERENCE_PREFIX) && header.contains(Constants.DEPENDENT_SEPARATOR);
    }

    public static String extractTargetAttributeName(String header, String className) {
        if (header.contains("[")) {
            int start = header.indexOf('[') + 1;
            int end = header.indexOf(']', start);
            if (end > start) {
                return header.substring(start, end);
            }
        }
        return decapitalize(className) + "_";
    }

    private static String decapitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }
    
    public static void main(String[] args) {
        System.out.println(extractTargetAttributeName("Patologia.nome", "Evento"));
    }

}
