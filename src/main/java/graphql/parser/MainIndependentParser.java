package graphql.parser;

import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataCell;
import graphql.model.DataRow;
import graphql.model.DataSheet;
import graphql.model.Header;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Excel sheets for main independent data and generates corresponding
 * GraphQL create mutations.
 */
public class MainIndependentParser implements DataParser {

    @Override
    public List<String> parseAndGenerateMutations(
            DataSheet sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder, // Not used for independent, but kept for interface consistency
            GraphQLService graphQLService) { // Not used for independent, but kept for interface consistency

        String objectName = sheetData.getName();
        List<DataRow> rows = sheetData.getDataRows();
        List<Header> headers = sheetData.getHeaders();

        return buildMutations(objectName, headers, rows, mutationBuilder);
    }

    private List<String> buildMutations(String objectName, List<Header> headers, List<DataRow> rows, GraphQLMutationBuilder mutationBuilder) {
        List<String> mutations = new ArrayList<>();
        for (DataRow row : rows) {
            Map<String, String> attributes = buildRowAttributes(headers, row);
            if (!attributes.isEmpty()) {
                String mutation = mutationBuilder.buildMainCreateMutation(objectName, attributes);
                mutations.add(mutation);
            }
        }
        return mutations;
    }

    private Map<String, String> buildRowAttributes(List<Header> headers, DataRow row) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (i >= row.getDataCells().size()) {
                continue;
            }
            Header header = headers.get(i);
            DataCell cell = row.get(i);
            if (cell != null && !cell.isBlank()) {
                attributes.put(header.getAttributeName(), cell.getFormattedValue());
            }
        }
        return attributes;
    }
}
