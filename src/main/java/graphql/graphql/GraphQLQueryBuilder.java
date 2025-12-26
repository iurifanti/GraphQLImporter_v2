package graphql.graphql;

import common.Utils;
import graphql.util.Constants;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Genera query GraphQL per recuperare gli _id di oggetti referenziati,
 * gestendo anche la suddivisione in batch dei valori richiesti.
 */
public class GraphQLQueryBuilder {

    /**
     * Crea una o più query per ottenere l'identificativo di record esistenti
     * filtrando per uno specifico attributo.
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

            String attributeValuesConc = Utils.join(batch, ", ");

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
