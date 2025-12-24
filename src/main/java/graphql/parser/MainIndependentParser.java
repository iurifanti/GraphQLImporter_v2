package graphql.parser;

import graphql.excel.ExcelSheetData;
import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import java.util.*;

/**
 * Parses Excel sheets for main independent data and generates corresponding
 * GraphQL create mutations.
 */
public class MainIndependentParser implements DataParser {

    @Override
    public List<String> parseAndGenerateMutations(
            ExcelSheetData sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder, // Not used for independent, but kept for interface consistency
            GraphQLService graphQLService) { // Not used for independent, but kept for interface consistency

        String objectName = sheetData.getSheetName();
        List<List<String>> rows = sheetData.getRows();
        List<String> headers = sheetData.getHeaders();

        return buildMutations(objectName, headers, rows, mutationBuilder);
    }

    private List<String> buildMutations(String objectName, List<String> headers, List<List<String>> rows, GraphQLMutationBuilder mutationBuilder) {
        List<String> mutations = new ArrayList<>();
        for (List<String> row : rows) {
            Map<String, String> attributes = buildRowAttributes(headers, row);
            if (!attributes.isEmpty()) {
                String mutation = mutationBuilder.buildCreateMutation(objectName, attributes);
                mutations.add(mutation);
            }
        }
        return mutations;
    }

    private Map<String, String> buildRowAttributes(List<String> headers, List<String> row) {
        Map<String, String> attributes = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String value = (i < row.size()) ? row.get(i) : "";
            if (value != null && !value.isEmpty()) {
                attributes.put(header, value);
            }
        }
        return attributes;
    }
}