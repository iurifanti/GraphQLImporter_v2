package graphql.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.util.Constants;
import graphql.util.LoggerUI;
import graphql.util.SslBypass;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Servizio per eseguire query GraphQL con supporto a endpoint
 * primario/secondario e autenticazione di base.
 */
public class GraphQLService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    public final String primaryEndpoint;
    public final String secondaryEndpoint;
    private final String basicAuthHeader;

    /**
     * Costruttore che istanzia i client con autenticazione base.
     *
     * @param primaryEndpoint URL endpoint primario
     * @param secondaryEndpoint URL endpoint secondario
     * @param username username per autenticazione base
     * @param password password per autenticazione base
     */
    public GraphQLService(String primaryEndpoint, String secondaryEndpoint, String username, String password) {
        this.primaryEndpoint = primaryEndpoint;
        this.secondaryEndpoint = secondaryEndpoint;
        String auth = username + ":" + password;
        this.basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
        SslBypass.disableSslVerificationIfNeeded();
    }

    /**
     * Esegue una query GraphQL sull'endpoint primario. Se riceve la tipica
     * risposta di errore "'Query' is undefined @ ..." riprova la stessa query
     * sull'endpoint secondario. Se la risposta contiene "errors", il metodo
     * logga l'errore in maniera human readable e lancia una RuntimeException
     * interrompendo l'esecuzione.
     */
    public Optional<String> executeQueryWithFallback(String query) throws IOException {
        try {
            LoggerUI.log("\n[GraphQL] Eseguo query sull'endpoint primario: " + primaryEndpoint);
            LoggerUI.log("[GraphQL] Query:\n" + query);
            Optional<String> response = executeQuery(query, primaryEndpoint);
            String endpointUsed = primaryEndpoint;
            if (response.isPresent() && containsUndefinedQueryError(response.get()) && secondaryEndpoint!=null) {
                LoggerUI.log("Primary endpoint failed with 'Query is undefined', retrying secondary endpoint.");
                LoggerUI.log("[GraphQL] Eseguo query sull'endpoint secondario: " + secondaryEndpoint);
                LoggerUI.log("[GraphQL] Query:\n" + query);
                response = executeQuery(query, secondaryEndpoint);
                endpointUsed = secondaryEndpoint;
                boolean onlyWarnings = isOnlyWarnings(response.get());
                logGraphQLErrors(response.get(), secondaryEndpoint);
                if (!onlyWarnings && containsUndefinedQueryError(response.get())) {
                    throw new RuntimeException("Errore GraphQL nella query: vedi dettagli sopra.");
                }
            } else if (response.isPresent() && containsGraphQLErrors(response.get())) {
                logGraphQLErrors(response.get(), primaryEndpoint);
                endpointUsed = primaryEndpoint;
                throw new RuntimeException("Errore GraphQL nella query: vedi dettagli sopra.");
            }
            if (response.isPresent()) {
                logGraphQLErrors(response.get(), endpointUsed);
            }
            return response;
        } catch (IOException e) {
            throw e;
        }
    }

    private boolean isOnlyWarnings(String responseJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseJson);
            if (root.has("errors")) {
                for (JsonNode err : root.get("errors")) {
                    String issueLevel = null;
                    if (err.has("extensions") && err.get("extensions").has("issueLevel")) {
                        issueLevel = err.get("extensions").get("issueLevel").asText();
                    }
                    if (!"WARNING".equalsIgnoreCase(issueLevel)) {
                        return false; // C'è almeno un errore "vero"
                    }
                }
                return true; // Tutti warning
            }
        } catch (Exception ignore) {
        }
        return false; // fallback: considera errore vero
    }

    /**
     * Estrae una mappa chiave-valore dal JSON di risposta dove chiave=primo
     * campo di ogni item, valore=campo "_id".
     */
    public Map<String, String> extractIdFromResponse(String jsonResponse, String attributeName) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode dataNode = rootNode.path("data");
        Map<String, String> res = new LinkedHashMap<>();
        if (dataNode != null && dataNode.isObject()) {
            JsonNode dynamicNode = dataNode.elements().hasNext() ? dataNode.elements().next() : null;
            if (dynamicNode != null) {
                JsonNode itemsNode = dynamicNode.path("items");
                if (itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        String key = null;
                        for (Iterator<Map.Entry<String, JsonNode>> fields = item.fields(); fields.hasNext();) {
                            Map.Entry<String, JsonNode> f = fields.next();
                            if (attributeName.equals(f.getKey())) {
                                key = f.getValue().asText();
                                break;
                            }
                        }
                        JsonNode idNode = item.get(Constants.ID);
                        if (key != null && idNode != null && idNode.isTextual()) {
                            res.put(key, idNode.asText());
                        }
                    }
                }
            }
        }
        return res;
    }

    private boolean containsUndefinedQueryError(String jsonResponse) {
        LoggerUI.log("jsonResponse: " + jsonResponse);
        return jsonResponse != null && jsonResponse.contains("\\u0027Query\\u0027 is undefined @");
    }

    /**
     * Controlla se la risposta contiene errori GraphQL.
     */
    private boolean containsGraphQLErrors(String jsonResponse) {
        return jsonResponse != null && jsonResponse.contains("\"errors\"");
    }

    private void logGraphQLErrors(String responseJson, String endpoint) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseJson);
            if (root.has("errors")) {
                JsonNode errors = root.get("errors");
                for (JsonNode err : errors) {
                    // Cerca il livello dell'errore
                    String issueLevel = null;
                    if (err.has("extensions") && err.get("extensions").has("issueLevel")) {
                        issueLevel = err.get("extensions").get("issueLevel").asText();
                    }

                    String msg = err.has("message") ? err.get("message").asText() : "Nessun messaggio";
                    if ("WARNING".equalsIgnoreCase(issueLevel)) {
                        LoggerUI.log("⚠️ WARNING GraphQL sull'endpoint " + endpoint + ": " + msg);
                    } else {
                        LoggerUI.log("❌ Errore GraphQL sull'endpoint: " + endpoint);
                        LoggerUI.log("  • Messaggio: " + msg);
                        // Dettagli posizione
                        if (err.has("locations")) {
                            for (JsonNode loc : err.get("locations")) {
                                String line = loc.has("line") ? loc.get("line").asText() : "?";
                                String col = loc.has("column") ? loc.get("column").asText() : "?";
                                LoggerUI.log("    (Linea: " + line + ", Colonna: " + col + ")");
                            }
                        }
                        // Classificazione (se presente)
                        if (err.has("extensions") && err.get("extensions").has("classification")) {
                            LoggerUI.log("    Tipo errore: " + err.get("extensions").get("classification").asText());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LoggerUI.log("❌ Errore nel parsing dettagliato della risposta GraphQL sull'endpoint " + endpoint + ": " + ex.getMessage());
            LoggerUI.log("Risposta grezza:\n" + responseJson);
        }
    }

    private Optional<String> executeQuery(String query, String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", basicAuthHeader);
        con.setDoOutput(true);

        String body = "{\"query\": " + toJsonString(query) + "}";
        try (java.io.OutputStream os = con.getOutputStream()) {
            byte[] input = body.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int code = con.getResponseCode();
        java.io.InputStream is = (code >= 200 && code < 300) ? con.getInputStream() : con.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        return Optional.of(response.toString());
    }
    // Utility per escape della query

    private static String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
