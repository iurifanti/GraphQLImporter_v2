/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.model;

import graphql.util.Constants;

/**
 *
 * @author iurif
 */
public class Header {

    private static final String FORCE_QUOTATION_PREFIX = "§";
    private static final String EXT_REF_PREFIX = "*";
    private final String header;

    // quando scorro tutti i valori, se uno solo è un decimale allora forzo l'uso delle vergolette anche se uno di loro è intero
    private boolean inferredQuotations = false;

    public Header(String header) {
        this.header = header.replace(EXT_REF_PREFIX, "");
    }

    public void setInferredQuotations(boolean inferredQuotations) {
        this.inferredQuotations = inferredQuotations;
    }

    public String getHeader() {
        return header;
    }

    public boolean isReference() {
        return header.contains(Constants.DEPENDENT_SEPARATOR);
    }

    private String defaultRoleName() {
        return decapitalize(getReferenceClassName()) + "_";
    }

    public boolean isInferredQuotations() {
        return inferredQuotations;
    }

    public boolean isForcedQuotations() {
        return header.startsWith(FORCE_QUOTATION_PREFIX);
    }

    public String roleName() {
        if (!isReference()) {
            return null;
        }

        if (header.contains("[")) {
            int start = header.indexOf('[') + 1;
            int end = header.indexOf(']', start);
            if (end > start) {
                return header.substring(start, end);
            }
        }
        return defaultRoleName();
    }

    public String getReferenceClassName() {
        String cleaned = header.startsWith(Constants.REAL_PREFIX)
                ? header.substring(Constants.REAL_PREFIX.length())
                : header;
        String[] parts = cleaned.split("\\" + Constants.DEPENDENT_SEPARATOR);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Intestazione di riferimento esterno non valida: " + header);
        }
        return parts[0];
    }

    public String getReferenceAttributeName() {
        String cleaned = header.startsWith(Constants.REAL_PREFIX)
                ? header.substring(Constants.REAL_PREFIX.length())
                : header;
        String[] parts = cleaned.split("\\" + Constants.DEPENDENT_SEPARATOR);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Intestazione di riferimento esterno non valida: " + header);
        }
        return parts[1];
    }

    private static String decapitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    public String getAttributeName() {
        return header.replace(FORCE_QUOTATION_PREFIX, "");
    }

}
