/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.model;

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

    public String getValue() {
        return header;
    }

    public boolean isReference() {
        return header.contains(".");
    }

    private String defaultRoleName() {
        return decapitalize(getReferencedClassName()) + "_";
    }

    public boolean isInferredQuotations() {
        return inferredQuotations;
    }

    public boolean isForcedQuotations() {
        return header.startsWith(FORCE_QUOTATION_PREFIX);
    }

    public String getReferencedRoleName() {
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

    public String getReferencedClassName() {
        if (isReference()) {
            return header.replace(EXT_REF_PREFIX, "").split("\\.")[0].split("\\[")[0];
        } else {
            return null;
        }
    }

    public String getReferencedAttributeName() {
        if (isReference()) {
            return header.split("\\.")[1];
        } else {
            return null;
        }
    }

    private static String decapitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    public String getAttributeName() {
        if (isReference()) {
            return getReferencedRoleName();
        } else {
            return header.replace(FORCE_QUOTATION_PREFIX, "");
        }
    }

}
