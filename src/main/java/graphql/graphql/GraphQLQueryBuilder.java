package graphql.graphql;

import graphql.util.Constants;
import graphql.util.Escaper;
import java.math.BigDecimal;
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
     * @param objectName The name of the GraphQL object (e.g., "Dipartimento").
     * @param attributeName The name of the attribute to filter by (e.g.,
     * "nome").
     * @param attributeValues The value of the attribute (e.g.,
     * "Amministrazione").
     * @return The complete GraphQL query string.
     */
    public String buildGetIdQuery(String objectName, String attributeName, Set<String> attributeValues) {
        Objects.requireNonNull(objectName, "Object name cannot be null");
        Objects.requireNonNull(attributeName, "Attribute name cannot be null");
        Objects.requireNonNull(attributeValues, "Attribute value cannot be null");
        // Simple sanitization for string values in the query
//        String formattedAttributeValue = Escaper.escapeJsonString(attributeValues);

//        return String.format(
//                "query {\n"
//                + "  %s%s(%s: \"%s\") {\n"
//                + "    _id\n"
//                + "  }\n"
//                + "}",
//                objectName, Constants.GRAPHQL_GET_BY_PREFIX + capitalize(attributeName,true),
//                attributeName, formattedAttributeValue
//        );
        String attributeValuesList = attributeValues.stream().
                map(v -> Escaper.escapeJsonString(v)).
                map(v -> attributeName.equalsIgnoreCase(Constants.ID) ? new BigDecimal(v).toBigInteger().toString() : ("\"" + v + "\"")).
                collect(Collectors.joining(","));
        String res = String.format(
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
                objectName,
                attributeName, attributeValuesList,
                attributeName.equals(Constants.ID) ? "" : attributeName
        );
        return res;
    }
}
