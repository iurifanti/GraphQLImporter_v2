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
import java.util.Set;

public class MainDependentParser extends ExternalAttributeResolverParser {

    @Override
    public List<String> parseAndGenerateMutations(
            DataSheet sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService
    ) throws Exception {
        String objectName = sheetData.getName();
        List<DataRow> rows = sheetData.getDataRows();
        List<Header> headers = sheetData.getHeaders();

        Map<Header, Set<String>> attributesToResolve = collectExternalAttributes(headers, rows, null);
        resolveAllExternalAttributesBatched(attributesToResolve, queryBuilder, graphQLService);

        return buildMutations(objectName, headers, rows, mutationBuilder);
    }

    private List<String> buildMutations(String objectName, List<Header> headers, List<DataRow> rows, GraphQLMutationBuilder mutationBuilder) {
        List<String> mutations = new ArrayList<>();
        for (DataRow row : rows) {
            Map<String, String> attributes = resolveRowAttributes(headers, row);
            if (!attributes.isEmpty()) {
                String mutation = mutationBuilder.buildMainCreateMutation(objectName, attributes);
                mutations.add(mutation);
            }
        }
        return mutations;
    }

    private Map<String, String> resolveRowAttributes(List<Header> headers, DataRow row) {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (i >= row.getDataCells().size()) {
                continue;
            }
            Header header = headers.get(i);
            DataCell cell = row.get(i);
            if (cell == null || cell.isBlank()) {
                continue;
            }

            String value = cell.getFormattedValue();

            if (header.isReference()) {
                String cacheKey = buildAttrKey(header.getReferenceClassName(), header.getReferenceAttributeName(), value);
                String resolvedId = attr2id.get(cacheKey);

                if (resolvedId == null) {
                    throw new IllegalStateException("ID non trovato per " + cacheKey);
                }

                String targetAttributeName = header.roleName();
                resolved.put(targetAttributeName, resolvedId);
            } else {
                resolved.put(header.getAttributeName(), value);
            }
        }
        return resolved;
    }
}
