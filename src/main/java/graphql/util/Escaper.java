/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.util;

/**
 *
 * @author iurif
 */
public class Escaper {
    /**
     * Escapes a string to be safely embedded within a JSON string literal. This
     * is crucial to prevent JSON parsing errors when the GraphQL query itself
     * contains quotes or special characters.
     *
     * @param text The input string to escape.
     * @return The escaped string.
     */
    public static String escapeJsonString(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
