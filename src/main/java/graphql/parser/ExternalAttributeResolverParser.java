package graphql.parser;

import common.Utils;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.util.Constants;
import graphql.util.JsonUtils;
import graphql.util.LoggerUI;
import java.math.BigDecimal;
import java.util.*;

public abstract class ExternalAttributeResolverParser implements DataParser {

    protected static final int BATCH_SIZE = 1000;
    protected final Map<String, String> attr2id = new LinkedHashMap<>();
    private GraphQLQueryBuilder queryBuilder;
    private GraphQLService graphQLService;

    public ExternalAttributeResolverParser(GraphQLQueryBuilder queryBuilder, GraphQLService graphQLService) {
        this.queryBuilder = queryBuilder;
        this.graphQLService = graphQLService;
    }

    protected Map<ExternalAttribute, Set<String>> collectExternalAttributes(
            List<String> headers, List<List<String>> rows, Set<String> skipHeaders
    ) {
        Map<ExternalAttribute, Set<String>> result = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (skipHeaders != null && skipHeaders.contains(header)) {
                continue;
            }
            if (!ExternalAttribute.isExternalAttribute(header)) {
                continue;
            }

            ExternalAttribute extAttr = ExternalAttribute.parseExternalAttribute(header);
            Set<String> values = result.computeIfAbsent(extAttr, k -> new LinkedHashSet<>());
            for (List<String> row : rows) {
                if (i < row.size()) {
                    String value = row.get(i);
                    if (value != null && !value.isEmpty()) {
                        values.add(value);
                    }
                }
            }
        }
        return result;
    }

    protected void resolveAllExternalAttributesBatched(
            Map<ExternalAttribute, Set<String>> attributesToResolve,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService
    ) throws Exception {
        for (Map.Entry<ExternalAttribute, Set<String>> entry : attributesToResolve.entrySet()) {
            ExternalAttribute extAttr = entry.getKey();
            Set<String> values = entry.getValue();

            List<String> valueList = new ArrayList<>(values);
            int total = valueList.size();
            int processed = 0;
            for (int i = 0; i < total; i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, total);
                Set<String> batch = new LinkedHashSet<>(valueList.subList(i, end));
                Map<String, String> resolved = MainDependentParser.getIds(
                        queryBuilder, graphQLService, extAttr.className, extAttr.attributeName, batch
                );

                for (Map.Entry<String, String> idEntry : resolved.entrySet()) {
                    String key = buildAttrKey(extAttr.className, extAttr.attributeName, idEntry.getKey());
                    attr2id.put(key, idEntry.getValue());
                }
                processed += batch.size();
                LoggerUI.log("Resolved attribute " + extAttr.className + "." + extAttr.attributeName + " IDs: " + processed + " / " + total);
            }
        }
    }

    protected String buildAttrKey(String className, String attribute, String value) {
        String val = attribute.equalsIgnoreCase(Constants.ID) ? new BigDecimal(value).stripTrailingZeros().toString() : value;
        return className + attribute + val;
    }

    public Map<String, String> getIds(
            String objectName,
            String attributeName,
            Set<String> valuesToQuery
    ) throws Exception {

        List<String> batchedQueries = queryBuilder.buildGetIdQuery(objectName, attributeName, valuesToQuery);
        int size = valuesToQuery.size();
        LoggerUI.log("Retrieving values for " + size + " external object" + (size > 1 ? "s" : "") + "...");
        Map<String, String> value2id = new LinkedHashMap();
        List<String> duplicati = new LinkedList<>();
        for (String query : batchedQueries) {
            getIdsBatch(query).forEach((k, v) -> {
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

    private Map<String, String> getIdsBatch(String query) throws Exception {
        Optional<String> response;

        try {
            response = graphQLService.executeQueryWithFallback(query);
        } catch (Exception e) {
            throw new RuntimeException("Error executing GraphQL query for dependent ID: " + e.getMessage(), e);
        }

        if (!response.isPresent()) {
            throw new RuntimeException("No response received from GraphQL endpoint(s).");
        }
        Map<String, String> val2id;
        try {
            val2id = JsonUtils.value2id(response.get());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during ID extraction: " + e.getMessage(), e);
        }
        return val2id;
    }

}
