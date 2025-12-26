package graphql.parser;

import common.Utils;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataCell;
import graphql.model.DataRow;
import graphql.model.Header;
import graphql.util.Constants;
import graphql.util.JsonUtils;
import graphql.util.LoggerUI;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class ExternalAttributeResolverParser implements DataParser {

    protected static final int BATCH_SIZE = 1000;
    protected final Map<String, String> attr2id = new LinkedHashMap<>();

    protected Map<Header, Set<String>> collectExternalAttributes(
            List<Header> headers,
            List<DataRow> rows,
            Set<Header> skipHeaders
    ) {
        Map<Header, Set<String>> result = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            Header header = headers.get(i);
            if (skipHeaders != null && skipHeaders.contains(header)) {
                continue;
            }
            if (!header.isReference()) {
                continue;
            }

            Set<String> values = result.computeIfAbsent(header, k -> new LinkedHashSet<>());
            for (DataRow row : rows) {
                if (i < row.getDataCells().size()) {
                    DataCell cell = row.get(i);
                    if (cell != null && !cell.isBlank()) {
                        values.add(cell.getFormattedValue());
                    }
                }
            }
        }
        return result;
    }

    protected void resolveAllExternalAttributesBatched(
            Map<Header, Set<String>> attributesToResolve,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService
    ) throws Exception {
        for (Map.Entry<Header, Set<String>> entry : attributesToResolve.entrySet()) {
            Header extAttr = entry.getKey();
            Set<String> values = entry.getValue();

            List<String> valueList = new ArrayList<>(values);
            int total = valueList.size();
            int processed = 0;
            for (int i = 0; i < total; i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, total);
                Set<String> batch = new LinkedHashSet<>(valueList.subList(i, end));
                Map<String, String> resolved = getIds(
                        queryBuilder, graphQLService, extAttr.getReferenceClassName(), extAttr.getReferenceAttributeName(), batch
                );

                for (Map.Entry<String, String> idEntry : resolved.entrySet()) {
                    String key = buildAttrKey(extAttr.getReferenceClassName(), extAttr.getReferenceAttributeName(), idEntry.getKey());
                    attr2id.put(key, idEntry.getValue());
                }
                processed += batch.size();
                LoggerUI.log("ID risolti per l'attributo " + extAttr.getReferenceClassName() + "." + extAttr.getReferenceAttributeName() + ": " + processed + " / " + total);
            }
        }
    }

    protected String buildAttrKey(String className, String attribute, String value) {
        String val = attribute.equalsIgnoreCase(Constants.ID) ? new BigDecimal(value).stripTrailingZeros().toString() : value;
        return className + attribute + val;
    }

    public static Map<String, String> getIds(
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService,
            String objectName,
            String attributeName,
            Set<String> valuesToQuery
    ) throws Exception {

        List<String> batchedQueries = queryBuilder.buildGetIdQuery(objectName, attributeName, valuesToQuery);
        int size = valuesToQuery.size();
        LoggerUI.log("Recupero dei valori per " + size + " oggetto esterno" + (size != 1 ? "i" : "") + "...");
        Map<String, String> value2id = new LinkedHashMap<>();
        List<String> duplicati = new LinkedList<>();
        for (String query : batchedQueries) {
            getIdsBatch(graphQLService, query).forEach((k, v) -> {
                String prev = value2id.put(k, v);
                if (prev != null) {
                    duplicati.add(k);
                }
            });
        }
        if (!duplicati.isEmpty()) {
            throw new RuntimeException("Sono presenti duplicati per i seguenti valori: " + Utils.join(duplicati, ", "));
        }
        return value2id;
    }

    private static Map<String, String> getIdsBatch(GraphQLService graphQLService, String query) throws Exception {
        Optional<String> response;

        try {
            response = graphQLService.executeQueryWithFallback(query);
        } catch (Exception e) {
            throw new RuntimeException("Errore nell'esecuzione della query GraphQL per recuperare l'ID: " + e.getMessage(), e);
        }

        if (!response.isPresent()) {
            throw new RuntimeException("Nessuna risposta ricevuta dagli endpoint GraphQL.");
        }
        Map<String, String> val2id;
        try {
            val2id = JsonUtils.value2id(response.get());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Errore inatteso durante l'estrazione degli ID: " + e.getMessage(), e);
        }
        return val2id;
    }

}
