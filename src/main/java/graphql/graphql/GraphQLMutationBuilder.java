package graphql.graphql;

import graphql.util.Constants;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds GraphQL queries and mutations from provided data.
 */
public class GraphQLMutationBuilder {

    /**
     * Builds a GraphQL create mutation.
     *
     * @param objectName The name of the object to create (e.g.,
     * "Dipartimento").
     * @param attributes A map of attribute names to their values.
     * @return The GraphQL create mutation string.
     */
    public String buildCreateMutation(String objectName, Map<String, String> attributes) {
//        LoggerUI.log("attributes: " + attributes);
        String inputData = formatAttributes(attributes);
//        LoggerUI.log("inputData; " + inputData);
        return String.format("mutation { %s___create(data: { %s }) { " + Constants.ID + " } }", objectName, inputData);
    }

    /**
     * Builds a GraphQL update mutation for composition (nested object).
     *
     * @param parentObjectName The name of the parent object (e.g.,
     * "Dipartimento").
     * @param parentIdentifierAttribute The attribute used to identify the
     * parent (e.g., "nome").
     * @param parentIdentifierValue The value of the parent identifier (e.g.,
     * "Amministrazione").
     * @param compositionName The name of the composed object (e.g.,
     * "Dipendente").
     * @param compositionAttributes A map of attributes for the composed object.
     * @return The GraphQL update mutation string.
     */
    public String buildCompositionUpdateMutation(
            String parentObjectName,
            String parentIdentifierAttribute,
            String parentIdentifierValue,
            String compositionName,
            Map<String, String> compositionAttributes) {

        String inputData = formatAttributes(compositionAttributes);

        return String.format(
                "mutation { %s___update(data: { %s: %s , %s: { create: { %s } } }) { _id } }",
                parentObjectName,
                parentIdentifierAttribute,
                parentIdentifierValue,
                // Assumo che il nome del campo di relazione sia lo stesso del nome del tipo composto,
                // ma con la prima lettera minuscola o plurale a seconda della convenzione del tuo schema.
                // Qui userò il nome del tipo composto come nome del campo.
                compositionName, // es: "Dipendente"
                inputData
        );
    }

    /**
     * Formats a map of attributes into a GraphQL input string. Handles
     * blank/empty values and boolean conversion.
     *
     * @param attributes The map of attribute names to their values.
     * @return A string formatted for GraphQL input (e.g., "name: \"value\",
     * quantity: 10").
     */
    private String formatAttributes(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .filter(entry -> {
                    String value = entry.getValue();
                    // NON includere l'attributo se il valore è null o una stringa vuota/blank
                    return value != null && !value.trim().isEmpty();
                })
                .map(entry -> {
                    String attrName = entry.getKey();
                    String value = entry.getValue().trim(); // Rimuovi spazi extra
                    // Gestione dei booleani
                    String valBool = value.replace("()", "").toLowerCase();
                    if (Arrays.asList("true", "false").contains(valBool)) {
                        // Se il valore è "true" o "false" (case-sensitive), lo restituisce come booleano
                        return String.format("%s: %s", attrName, valBool);
                    } else if (isNumeric(value)) {
                        // Se il valore è numerico, lo restituisce direttamente (senza virgolette)
                        value = new BigDecimal(value).stripTrailingZeros().toPlainString();
                        if (value.contains(".") || attrName.contains(Constants.REAL_PREFIX)) {
                            value = "\"" + value + "\"";
                        }
                        return String.format("%s: %s", attrName, value);
                    } else {
                        // Per tutti gli altri valori (stringhe), li racchiude tra virgolette
                        // Eseguiamo l'escape delle virgolette interne
                        String escapedValue = value.replace("\"", "\\\"");
                        return String.format("%s: \"%s\"", attrName, escapedValue);
                    }
                })
                .collect(Collectors.joining(", ")).replace(Constants.REAL_PREFIX, "");
    }

    /**
     * Helper method to check if a string represents a number.
     *
     * @param str The string to check.
     * @return true if the string is a valid number (integer or decimal), false
     * otherwise.
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
