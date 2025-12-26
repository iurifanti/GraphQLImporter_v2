package graphql.graphql;

import graphql.util.Constants;
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
    public String buildMainCreateMutation(String objectName, Map<String, String> attributes) {
//        LoggerUI.log("attributes: " + attributes);
        String inputData = formatAttributes(attributes);
//        LoggerUI.log("inputData; " + inputData);
        return String.format("mutation { %s___create(data: { %s }) { " + Constants.ID + " } }", objectName, inputData);
    }

    /**
     * Builds a GraphQL update mutation for composition (nested object).
     *
     * @param wholeObjectName The name of the parent object (e.g.,
     * "Dipartimento").
     * @param wholeIdentifierAttributeName The attribute used to identify the
     * parent (e.g., "nome").
     * @param wholeID The value of the parent identifier (e.g.,
     * "Amministrazione").
     * @param compositionRoleName The name of the composed object (e.g.,
     * "Dipendente").
     * @param attributeName2formattedValues A map of attributes for the composed object.
     * @return The GraphQL update mutation string.
     */
    public String buildCompositionUpdateMutation(
            String wholeObjectName,
            String wholeID,
            String compositionRoleName,
            Map<String, String> attributeName2formattedValues) {

        String inputData = formatAttributes(attributeName2formattedValues);

        return String.format(
                "mutation { %s___update(data: { %s: %s , %s: { create: { %s } } }) { _id } }",
                wholeObjectName,
                Constants.ID,
                wholeID,
                compositionRoleName, // es: "Dipendente"
                inputData
        );
    }

    /**
     * Formats a map of attributes into a GraphQL input string. Handles
     * blank/empty values and boolean conversion.
     *
     * @param attributeName2formattedValue The map of attribute names to their
     * values.
     * @return A string formatted for GraphQL input (e.g., "name: \"value\",
     * quantity: 10").
     */
    private String formatAttributes(Map<String, String> attributeName2formattedValue) {
        return attributeName2formattedValue.entrySet().stream()
                .filter(entry -> {
                    String value = entry.getValue();
                    // NON includere l'attributo se il valore è null o una stringa vuota/blank
                    return value != null && !value.trim().isEmpty();
                })
                .map(entry -> {
                    String attrName = entry.getKey();
                    String value = entry.getValue().trim(); // Rimuovi spazi extra
//                    String escapedValue = value.replace("\"", "\\\"");
                    return String.format("%s: %s", attrName, value);
                })
                .collect(Collectors.joining(", "));
    }
}
