package graphql.parser;

import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataSheet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for parsing Excel sheet data and generating GraphQL mutations.
 */
public interface DataParser {

    Map<String, String> attribute2type = new LinkedHashMap();

    /**
     * Processes the given Excel sheet data and generates a list of GraphQL
     * mutations. Implementations will differ based on the type of data (main
     * independent, dependent, composition).
     *
     * @param sheetData The data from the Excel sheet.
     * @param mutationBuilder The GraphQLMutationBuilder instance.
     * @param queryBuilder The GraphQLQueryBuilder instance.
     * @param graphQLService The GraphQLService instance for interacting with
     * the server.
     * @return A list of generated GraphQL mutation strings.
     * @throws Exception if an error occurs during parsing or ID retrieval.
     */
    public List<String> parseAndGenerateMutations(
            DataSheet sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService) throws Exception;

}
