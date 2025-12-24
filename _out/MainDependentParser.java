package graphql.parser;

import graphql.excel.ExcelSheetData;
import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import java.util.*;

public class MainDependentParser extends ExternalAttributeResolverParser {

    @Override
    public List<String> parseAndGenerateMutations(
            ExcelSheetData sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService
    ) throws Exception {
        String objectName = sheetData.getSheetName();
        List<List<String>> rows = sheetData.getRows();
        List<String> headers = sheetData.getHeaders();

        // 1. Raccogli i valori unici per ogni attributo esterno
        Map<ExternalAttribute, Set<String>> attributesToResolve = collectExternalAttributes(headers, rows, null);

        // 2. Risolvi ciascun attributo esterno separatamente in batch
        resolveAllExternalAttributesBatched(attributesToResolve, queryBuilder, graphQLService);

        // 3. Costruisci mutazioni
        return buildMutations(objectName, headers, rows, mutationBuilder);
    }

    private List<String> buildMutations(String objectName, List<String> headers, List<List<String>> rows, GraphQLMutationBuilder mutationBuilder) {
        List<String> mutations = new ArrayList<>();
        for (List<String> row : rows) {
            Map<String, String> attributes = resolveRowAttributes(headers, row);
            if (!attributes.isEmpty()) {
                String mutation = mutationBuilder.buildCreateMutation(objectName, attributes);
                mutations.add(mutation);
            }
        }
        return mutations;
    }

    private Map<String, String> resolveRowAttributes(List<String> headers, List<String> row) {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String value = (i < row.size()) ? row.get(i) : "";

            if (ExternalAttribute.isExternalAttribute(header)) {
                ExternalAttribute extAttr = ExternalAttribute.parseExternalAttribute(header);
                String cacheKey = buildAttrKey(extAttr.className, extAttr.attributeName, value);
                String resolvedId = attr2id.get(cacheKey);

                if (resolvedId == null) {
                    throw new IllegalStateException("ID non trovato per " + cacheKey);
                }

                String targetAttributeName = ExternalAttribute.extractTargetAttributeName(header, extAttr.className);
                resolved.put(targetAttributeName, resolvedId);
            } else if (value != null && !value.isEmpty()) {
                resolved.put(header, value);
            }
        }
        return resolved;
    }
}