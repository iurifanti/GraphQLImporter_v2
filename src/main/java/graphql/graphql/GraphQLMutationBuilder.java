package graphql.graphql;

import graphql.util.Constants;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Responsabile della generazione delle mutation GraphQL a partire dai dati
 * estratti dai fogli Excel.
 */
public class GraphQLMutationBuilder {

    /**
     * Costruisce una mutation di creazione per un'entità principale.
     */
    public String buildMainCreateMutation(String objectName, Map<String, String> attributes) {
        String inputData = formatAttributes(attributes);
        return String.format("mutation { %s___create(data: { %s }) { " + Constants.ID + " } }", objectName, inputData);
    }

    /**
     * Genera una mutation di aggiornamento per aggiungere elementi di una
     * composizione al "whole" indicato.
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
     * Converte la mappa degli attributi in una stringa GraphQL pronta per
     * essere inserita nella mutation, ignorando i campi vuoti o nulli.
     */
    private String formatAttributes(Map<String, String> attributeName2formattedValue) {
        return attributeName2formattedValue.entrySet().stream()
                .filter(entry -> {
                    String value = entry.getValue();
                    // Esclude l'attributo se il valore non contiene informazioni utili
                    return value != null && !value.trim().isEmpty();
                })
                .map(entry -> {
                    String attrName = entry.getKey();
                    String value = entry.getValue().trim(); // Rimuove spazi superflui
                    return String.format("%s: %s", attrName, value);
                })
                .collect(Collectors.joining(", "));
    }
}
