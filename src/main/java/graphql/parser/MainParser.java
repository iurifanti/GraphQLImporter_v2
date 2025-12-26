package graphql.parser;

import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataRow;
import graphql.model.DataSheet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainParser extends ExternalAttributeResolver implements MutationParser {

    @Override
    public List<String> parseAndGenerateMutations(
            DataSheet sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService
    ) throws Exception {
        resolveAllExternalAttributes(queryBuilder, graphQLService, sheetData);

        return buildMainMutations(sheetData, mutationBuilder);
    }

    private List<String> buildMainMutations(DataSheet sheetData, GraphQLMutationBuilder mutationBuilder) {
        List<String> mutations = new ArrayList<>();
        for (DataRow row : sheetData.getDataRows()) {
            Map<String, String> attributes = buildMainAttributes(sheetData, row);
            if (!attributes.isEmpty()) {
                String mutation = mutationBuilder.buildMainCreateMutation(sheetData.getName(), attributes);
                mutations.add(mutation);
            }
        }
        return mutations;
    }

}
