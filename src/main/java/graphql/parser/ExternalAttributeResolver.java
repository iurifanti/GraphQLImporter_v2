package graphql.parser;

import common.Utils;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataCell;
import graphql.model.DataRow;
import graphql.model.DataSheet;
import graphql.model.Header;
import graphql.util.JsonUtils;
import graphql.util.LoggerUI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class ExternalAttributeResolver {

    protected Map<Header, Map<String, String>> header2value2id = new LinkedHashMap();

    protected Map<String, String> buildMainAttributes(DataSheet sheetData, DataRow rowData) {
        return buildCompositionAttributes(sheetData, rowData, null);
    }

    protected Map<String, String> buildCompositionAttributes(DataSheet sheetData, DataRow rowData, Header parentHeader) {

        Map<String, String> mutationAttributes = new LinkedHashMap<>();
        for (DataCell dataCell : rowData.getDataCells()) {
            if (dataCell.getHeader().equals(parentHeader) || dataCell.isBlank()) {
                continue;
            }
            if (dataCell.getHeader().isReference()) {
                if (dataCell.isBlank()) {
                    continue;
                }
                String resolvedId = getResolvedId(dataCell);
                if (resolvedId == null) {
                    LoggerUI.log("Errore: impossibile risolvere l'ID per il riferimento esterno '" + dataCell
                            + "' nella colonna '" + dataCell.getHeader().getValue() + "' riga " + (sheetData.getDataRows().indexOf(rowData) + 2));
                    continue;
                }
                mutationAttributes.put(dataCell.getHeader().getAttributeName(), resolvedId);
            } else {
                mutationAttributes.put(dataCell.getHeader().getAttributeName(), dataCell.getFormattedValue());
            }
        }
        return mutationAttributes;
    }

    protected void resolveAllExternalAttributes(
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService,
            DataSheet sheetData) throws Exception {
        for (Header header : sheetData.externalHeaders()) {
            Set<String> values = extractValuesForHeader(sheetData, header);
            Map<String, String> values2id = getIds(queryBuilder, graphQLService, header.getReferencedClassName(), header.getReferencedAttributeName(), values);
            header2value2id.put(header, values2id);
        }
    }

    protected Set<String> extractValuesForHeader(DataSheet sheetData, Header header) {
        Set<String> values = new LinkedHashSet<>();
        for (DataRow row : sheetData.getDataRows()) {
            DataCell cell = row.get(header);
            if (cell != null) {
                values.add(cell.getFormattedValue());
            }
        }
        return values;
    }

    protected String getResolvedId(DataCell dataCell) {
        return header2value2id.get(dataCell.getHeader()).get(dataCell.getValue());
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
        LoggerUI.log("Recupero degli ID per " + size + " valori...");
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
