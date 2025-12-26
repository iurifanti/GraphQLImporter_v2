package graphql.parser;

import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataCell;
import graphql.model.DataRow;
import graphql.model.DataSheet;
import graphql.model.Header;
import graphql.util.LoggerUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CompositionParser extends ExternalAttributeResolver implements MutationParser {

    private void validateHeaders(DataSheet sheetData) {
        if (sheetData.getHeaders().isEmpty()) {
            throw new IllegalArgumentException("Il foglio di composizione '" + sheetData.getName() + "' deve avere delle intestazioni.");
        }
    }

    @Override
    public List<String> parseAndGenerateMutations(
            DataSheet sheetData,
            GraphQLMutationBuilder mutationBuilder,
            GraphQLQueryBuilder queryBuilder,
            GraphQLService graphQLService) throws Exception {

        validateHeaders(sheetData);

        List<Header> externalHeaders = sheetData.getHeaders().stream()
                .filter(Header::isReference)
                .collect(Collectors.toList());
        if (externalHeaders.isEmpty()) {
            throw new IllegalArgumentException(
                    "La composizione '" + sheetData.getName() + "' deve contenere un riferimento al whole (es. '*Parent.attribute').");
        }

        resolveAllExternalAttributes(queryBuilder, graphQLService, sheetData);

        Header parentHeader = externalHeaders.get(0);

        List<String> mutations = new ArrayList<>();

        for (DataRow rowData : sheetData.getDataRows()) {

            DataCell parentCell = rowData.get(parentHeader);
            if (parentCell == null) {
                LoggerUI.log("Errore: identificativo del genitore mancante nella riga " + (sheetData.getDataRows().indexOf(rowData) + 2) + " del foglio '"
                        + sheetData.getName() + "'. Riga ignorata.");
                continue;
            }

            String parentId = getResolvedId(parentCell);
            if (parentId == null) {
                LoggerUI.log("Errore: impossibile risolvere l'ID per il valore del genitore '" + parentCell + "' nella riga " + (sheetData.getDataRows().indexOf(rowData) + 2));
                continue;
            }

            Map<String, String> compositionAttributes = buildCompositionAttributes(sheetData, rowData, parentHeader);

            String mutation = mutationBuilder.buildCompositionUpdateMutation(
                    parentHeader.getReferencedClassName(),
                    parentId,
                    sheetData.getCompositionRoleName(),
                    compositionAttributes
            );

            mutations.add(mutation);
        }

        return mutations;
    }
}
