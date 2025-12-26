package graphql.graphql;

import graphql.util.Constants;
import graphql.util.JsonUtils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds GraphQL query strings for fetching data, specifically for getting IDs
 * of dependent objects.
 */
public class GraphQLQueryBuilder {

    /**
     * Builds a GraphQL query to retrieve the _id of an object based on a
     * specific attribute.
     *
     * @param referencedClassName The name of the GraphQL object (e.g.,
     * "Dipartimento").
     * @param referencedAttributeName The name of the attribute to filter by
     * (e.g., "nome").
     * @param referencedAttributeFormattedValues The value of the attribute
     * (e.g., "Amministrazione").
     * @return The complete GraphQL query string.
     */
    public List<String> buildGetIdQuery(String referencedClassName, String referencedAttributeName, Set<String> referencedAttributeFormattedValues) {
        Objects.requireNonNull(referencedClassName, "Object name cannot be null");
        Objects.requireNonNull(referencedAttributeName, "Attribute name cannot be null");
        Objects.requireNonNull(referencedAttributeFormattedValues, "Attribute value cannot be null");

        List<String> queries = new LinkedList<>();

        int batchSize = 100;

        List<String> list = new ArrayList<>(referencedAttributeFormattedValues);
        for (int i = 0; i < referencedAttributeFormattedValues.size(); i += batchSize) {

            int end = Math.min(i + batchSize, referencedAttributeFormattedValues.size());
            List<String> batch = list.subList(i, end);
            
            String attributeValuesConc = batch.stream().
                    map(v -> JsonUtils.escapeJsonString(v)).
                    collect(Collectors.joining(","));

            String query = String.format(
                    "query {\n"
                    + "  %s___getPage(options: {\n"
                    + "    filter: {\n"
                    + "      %s___in: [%s]\n"
                    + "    }\n"
                    + "    offset: 0,\n"
                    + "    next: 999999\n"
                    + "  }) {\n"
                    + "    items {\n"
                    + "      %s\n"
                    + "      _id\n"
                    + "    }\n"
                    + "  }\n"
                    + "}",
                    referencedClassName,
                    referencedAttributeName,
                    attributeValuesConc,
                    referencedAttributeName.equals(Constants.ID) ? "" : referencedAttributeName
            );
            queries.add(query);
        }
        return queries;
    }

}
