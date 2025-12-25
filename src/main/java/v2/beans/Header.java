/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

/**
 *
 * @author iurif
 */
public class Header {

    private final String header;

    public Header(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public boolean isReference() {
        return header.contains(".");
    }

    private String defaultRoleName() {
        return decapitalize(header.split("\\.")[0]) + "_";
    }

    public boolean isForcedQuotations() {
        return header.startsWith("§");
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

    private static String decapitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

}
