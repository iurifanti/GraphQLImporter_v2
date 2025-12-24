package graphql.parser;

import graphql.excel.ExcelSheetData;
import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.util.Constants;
import graphql.util.LoggerUI;
import java.util.*;
import java.util.stream.Collectors;

public class CompositionParser extends ExternalAttributeResolverParser {

    @Override
    public List<String> parseAndGenerateMutations(
            ExcelSheetData sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService) throws Exception {

        validateHeaders(sheetData);

        String fullSheetName = sheetData.getSheetName();
        String compositionName = fullSheetName.substring(Constants.COMPOSITION_PREFIX.length());

        // Trova tutti i riferimenti esterni
        List<String> externalHeaders = sheetData.getHeaders().stream()
                .filter(ExternalAttribute::isExternalAttribute)
                .collect(Collectors.toList());
        if (externalHeaders.isEmpty()) {
            throw new IllegalArgumentException("Composition sheet '" + sheetData.getSheetName()
                    + "' must contain at least one parent identifier header (e.g., '*Parent.attribute').");
        }

        String parentHeader = externalHeaders.get(0);
        ExternalAttribute parentAttr = ExternalAttribute.parseExternalAttribute(parentHeader);

        // Raccogli tutti i valori unici dell’attributo genitore
        Set<String> parentValues = extractValuesForHeader(sheetData, parentHeader);

        // Risolvi tutti gli ID parent in batch
        resolveParentIdsBatched(parentAttr, parentValues, queryBuilder, graphQLService);

        // Raccogli riferimenti esterni secondari (tutti tranne il parentHeader)
        Set<String> skipHeaders = new HashSet<>();
        skipHeaders.add(parentHeader);
        Map<ExternalAttribute, Set<String>> secondaryExternalAttrToValues =
                collectExternalAttributes(sheetData.getHeaders(), sheetData.getRows(), skipHeaders);

        // Risolvi anche questi in batch, come in MainDependentParser
        resolveAllExternalAttributesBatched(secondaryExternalAttrToValues, queryBuilder, graphQLService);

        // Genera mutazioni usando anche gli id risolti per i riferimenti esterni secondari
        return buildCompositionMutations(sheetData, compositionName, parentHeader, parentAttr, mutationBuilder);
    }

    private void validateHeaders(ExcelSheetData sheetData) {
        if (sheetData.getHeaders().isEmpty()) {
            throw new IllegalArgumentException("Composition sheet '" + sheetData.getSheetName() + "' must have headers.");
        }
    }

    private Set<String> extractValuesForHeader(ExcelSheetData sheetData, String header) {
        Set<String> values = new LinkedHashSet<>();
        int index = sheetData.getHeaders().indexOf(header);
        if (index < 0) {
            return values;
        }
        for (List<String> row : sheetData.getRows()) {
            if (index < row.size()) {
                String value = row.get(index);
                if (value != null && !value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private void resolveParentIdsBatched(ExternalAttribute parentAttr, Set<String> values,
            GraphQLQueryBuilder queryBuilder, GraphQLService graphQLService) {

        List<String> valueList = new ArrayList<>(values);
        int total = valueList.size();
        int processed = 0;
        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);
            Set<String> batch = new LinkedHashSet<>(valueList.subList(i, end));
            Map<String, String> resolved = MainDependentParser.getIds(
                    queryBuilder,
                    graphQLService,
                    parentAttr.className,
                    parentAttr.attributeName,
                    batch
            );
            for (Map.Entry<String, String> entry : resolved.entrySet()) {
                String key = buildAttrKey(parentAttr.className, parentAttr.attributeName, entry.getKey());
                attr2id.put(key, entry.getValue());
            }
            processed += batch.size();
            LoggerUI.log("Resolved parent IDs: " + processed + " / " + total);
        }
    }

    private List<String> buildCompositionMutations(
            ExcelSheetData sheetData,
            String compositionName,
            String parentHeader,
            ExternalAttribute parentAttr,
            GraphQLMutationBuilder mutationBuilder) {

        List<String> mutations = new ArrayList<>();
        List<String> headers = sheetData.getHeaders();

        for (List<String> row : sheetData.getRows()) {
            Map<String, String> rowData = buildRowData(sheetData, row);

            String parentValue = rowData.get(parentHeader);
            if (parentValue == null || parentValue.isEmpty()) {
                LoggerUI.log("Error: Parent identifier missing in row " + (sheetData.getRows().indexOf(row) + 2) + " of sheet '" + sheetData.getSheetName() + "'. Skipping.");
                continue;
            }

            String parentKey = buildAttrKey(parentAttr.className, parentAttr.attributeName, parentValue);
            String parentId = attr2id.get(parentKey);
            if (parentId == null) {
                LoggerUI.log("Error: Unable to resolve ID for parent value '" + parentValue + "' in row " + (sheetData.getRows().indexOf(row) + 2));
                continue;
            }

            // Costruisci attributi per la mutation, risolvendo anche i riferimenti esterni secondari
            Map<String, String> compositionAttributes = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                if (header.equals(parentHeader)) {
                    continue;
                }

                String value = rowData.get(header);
                if (ExternalAttribute.isExternalAttribute(header)) {
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    ExternalAttribute extAttr = ExternalAttribute.parseExternalAttribute(header);
                    String key = buildAttrKey(extAttr.className, extAttr.attributeName, value);
                    String resolvedId = attr2id.get(key);
                    if (resolvedId == null) {
                        LoggerUI.log("Error: Unable to resolve ID for external reference '" + value
                                + "' in column '" + header + "' row " + (sheetData.getRows().indexOf(row) + 2));
                        continue;
                    }
                    // Come in MainDependentParser: usa il nome target (non header!)
                    String targetAttributeName = ExternalAttribute.extractTargetAttributeName(header, extAttr.className);
                    compositionAttributes.put(targetAttributeName, resolvedId);
                } else {
                    compositionAttributes.put(header, value == null ? "" : value);
                }
            }

            String mutation = mutationBuilder.buildCompositionUpdateMutation(
                    parentAttr.className,
                    Constants.ID,
                    parentId,
                    compositionName,
                    compositionAttributes
            );

            mutations.add(mutation);
        }

        return mutations;
    }

    private Map<String, String> buildRowData(ExcelSheetData sheetData, List<String> row) {
        Map<String, String> rowData = new LinkedHashMap<>();
        List<String> headers = sheetData.getHeaders();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String value = (i < row.size()) ? row.get(i) : "";
            if (value == null) {
                value = "";
            }
            rowData.put(header, value);
        }

        return rowData;
    }
}