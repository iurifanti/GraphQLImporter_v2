package graphql.parser;

import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataCell;
import graphql.model.DataRow;
import graphql.model.DataSheet;
import graphql.model.Header;
import graphql.util.Constants;
import graphql.util.LoggerUI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CompositionParser extends ExternalAttributeResolverParser {

    @Override
    public List<String> parseAndGenerateMutations(
            DataSheet sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService) throws Exception {

        validateHeaders(sheetData);

        String compositionName = sheetData.getRoleName();

        List<Header> externalHeaders = sheetData.getHeaders().stream()
                .filter(Header::isReference)
                .collect(Collectors.toList());
        if (externalHeaders.isEmpty()) {
            throw new IllegalArgumentException(
                    "Il foglio di composizione '" + sheetData.getName() + "' deve contenere almeno un riferimento al genitore (es. '*Parent.attribute').");
        }

        Header parentHeader = externalHeaders.get(0);

        Set<String> parentValues = extractValuesForHeader(sheetData, parentHeader);
        resolveParentIdsBatched(parentHeader, parentValues, queryBuilder, graphQLService);

        Set<Header> skipHeaders = new HashSet<>();
        skipHeaders.add(parentHeader);
        Map<Header, Set<String>> secondaryExternalAttrToValues =
                collectExternalAttributes(sheetData.getHeaders(), sheetData.getDataRows(), skipHeaders);

        resolveAllExternalAttributesBatched(secondaryExternalAttrToValues, queryBuilder, graphQLService);

        return buildCompositionMutations(sheetData, compositionName, parentHeader, mutationBuilder);
    }

    private void validateHeaders(DataSheet sheetData) {
        if (sheetData.getHeaders().isEmpty()) {
            throw new IllegalArgumentException("Il foglio di composizione '" + sheetData.getName() + "' deve avere delle intestazioni.");
        }
    }

    private Set<String> extractValuesForHeader(DataSheet sheetData, Header header) {
        Set<String> values = new LinkedHashSet<>();
        int index = sheetData.getHeaders().indexOf(header);
        if (index < 0) {
            return values;
        }
        for (DataRow row : sheetData.getDataRows()) {
            if (index < row.getDataCells().size()) {
                DataCell cell = row.get(index);
                if (cell != null && !cell.isBlank()) {
                    values.add(cell.getFormattedValue());
                }
            }
        }
        return values;
    }

    private void resolveParentIdsBatched(Header parentHeader, Set<String> values,
            GraphQLQueryBuilder queryBuilder, GraphQLService graphQLService) throws Exception {

        List<String> valueList = new ArrayList<>(values);
        int total = valueList.size();
        int processed = 0;
        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);
            Set<String> batch = new LinkedHashSet<>(valueList.subList(i, end));
            Map<String, String> resolved = getIds(
                    queryBuilder,
                    graphQLService,
                    parentHeader.getReferenceClassName(),
                    parentHeader.getReferenceAttributeName(),
                    batch
            );
            for (Map.Entry<String, String> entry : resolved.entrySet()) {
                String key = buildAttrKey(parentHeader.getReferenceClassName(), parentHeader.getReferenceAttributeName(), entry.getKey());
                attr2id.put(key, entry.getValue());
            }
            processed += batch.size();
            LoggerUI.log("ID del genitore risolti: " + processed + " / " + total);
        }
    }

    private List<String> buildCompositionMutations(
            DataSheet sheetData,
            String compositionName,
            Header parentHeader,
            GraphQLMutationBuilder mutationBuilder) {

        List<String> mutations = new ArrayList<>();
        List<Header> headers = sheetData.getHeaders();

        for (DataRow row : sheetData.getDataRows()) {
            Map<Header, String> rowData = buildRowData(headers, row);

            String parentValue = rowData.get(parentHeader);
            if (parentValue == null || parentValue.isEmpty()) {
                LoggerUI.log("Errore: identificativo del genitore mancante nella riga " + (sheetData.getDataRows().indexOf(row) + 2) + " del foglio '"
                        + sheetData.getName() + "'. Riga ignorata.");
                continue;
            }

            String parentKey = buildAttrKey(parentHeader.getReferenceClassName(), parentHeader.getReferenceAttributeName(), parentValue);
            String parentId = attr2id.get(parentKey);
            if (parentId == null) {
                LoggerUI.log("Errore: impossibile risolvere l'ID per il valore del genitore '" + parentValue + "' nella riga " + (sheetData.getDataRows().indexOf(row) + 2));
                continue;
            }

            Map<String, String> compositionAttributes = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                Header header = headers.get(i);
                if (header.equals(parentHeader)) {
                    continue;
                }

                String value = rowData.get(header);
                if (header.isReference()) {
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    String key = buildAttrKey(header.getReferenceClassName(), header.getReferenceAttributeName(), value);
                    String resolvedId = attr2id.get(key);
                    if (resolvedId == null) {
                        LoggerUI.log("Errore: impossibile risolvere l'ID per il riferimento esterno '" + value
                                + "' nella colonna '" + header.getHeader() + "' riga " + (sheetData.getDataRows().indexOf(row) + 2));
                        continue;
                    }
                    String targetAttributeName = header.roleName();
                    compositionAttributes.put(targetAttributeName, resolvedId);
                } else {
                    compositionAttributes.put(header.getAttributeName(), value == null ? "" : value);
                }
            }

            String mutation = mutationBuilder.buildCompositionUpdateMutation(
                    parentHeader.getReferenceClassName(),
                    Constants.ID,
                    parentId,
                    compositionName,
                    compositionAttributes
            );

            mutations.add(mutation);
        }

        return mutations;
    }

    private Map<Header, String> buildRowData(List<Header> headers, DataRow row) {
        Map<Header, String> rowData = new LinkedHashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            Header header = headers.get(i);
            String value = (i < row.getDataCells().size() && row.get(i) != null) ? row.get(i).getFormattedValue() : "";
            rowData.put(header, value);
        }

        return rowData;
    }
}
