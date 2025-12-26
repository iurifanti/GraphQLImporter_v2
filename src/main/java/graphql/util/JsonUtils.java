package graphql.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.Utils;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility per la gestione di stringhe e risposte JSON utilizzate durante la
 * costruzione ed esecuzione delle query GraphQL.
 */
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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

    public String extractValueFromResponse(String jsonResponse, String attributeName) throws IOException {
        JsonNode root = objectMapper.readTree(jsonResponse);
        return findValueRecursive(root, attributeName);
    }

    private String findValueRecursive(JsonNode node, String attributeName) {
        // Se il nodo è l'attributo e contiene un valore semplice restituisce subito il testo
        if (node.has(attributeName)) {
            JsonNode val = node.get(attributeName);
            if (val.isValueNode()) {  // string, number, boolean
                return val.asText();
            }
        }

        // Se è un oggetto o un array, continua la ricerca sui nodi figli
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                String result = findValueRecursive(child, attributeName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public static Map<String, String> value2id(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = new LinkedHashMap<>();
        JsonNode items = mapper.readTree(json)
                .path("data")
                .elements().next()
                .path("items");
        List<String> duplicati = new LinkedList<>();
        for (JsonNode item : items) {
            String k = item.elements().next().asText();
            String v = item.path(Constants.ID).asText();
            String previous = map.put(k, v);
            if (previous != null) {
                duplicati.add(k);
            }
        }
        if (!duplicati.isEmpty()) {
            throw new RuntimeException("Sono presenti duplicati per i seguenti valori: " + Utils.join(duplicati, ", "));
        }
        return map;
    }
}
