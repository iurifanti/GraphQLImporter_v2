package graphql.graphql;

import graphql.excel.ExcelSheetData;
import graphql.parser.ExternalAttribute;
import graphql.util.Constants;
import java.util.*;

/**
 * Genera una mappa <ComposizioneRadice, MutationUpdate> dove la mutation update
 * è annidata secondo la catena di dipendenze tra composizioni.
 * La create del main NON viene modificata né annidata.
 *
 * Versione Java 8, leggibile e manutenibile.
 */
public class NestedCompositionUpdateBuilder {

    private final Map<String, ExcelSheetData> compositionSheets = new HashMap<>();
    private final Map<String, String> compositionParentMap = new HashMap<>();

    public NestedCompositionUpdateBuilder(List<ExcelSheetData> allSheets) {
        for (ExcelSheetData sheet : allSheets) {
            if (isCompositionSheet(sheet)) {
                compositionSheets.put(sheet.getSheetName(), sheet);
                String parentHeader = sheet.getHeaders().get(0);
                ExternalAttribute extAttr = ExternalAttribute.parseExternalAttribute(parentHeader);
                compositionParentMap.put(sheet.getSheetName(), extAttr.className); // nome della classe (main o composizione padre)
            }
        }
    }

    private boolean isCompositionSheet(ExcelSheetData sheet) {
        return sheet.getSheetName().startsWith(Constants.COMPOSITION_PREFIX);
    }

    /**
     * Restituisce una mappa <ComposizioneRadice, MutationUpdate> con le mutazioni annidate.
     * @param allSheets tutti i fogli Excel
     * @return mappa <nomeComposizioneRadice, mutationUpdateString>
     */
    public Map<String, String> buildNestedCompositionsUpdateMutations(List<ExcelSheetData> allSheets) {
        Map<String, String> result = new LinkedHashMap<>();

        // Trova tutti i fogli main (no #, no mapping)
        Set<String> mainNames = new HashSet<>();
        for (ExcelSheetData sheet : allSheets) {
            String name = sheet.getSheetName();
            if (!name.startsWith(Constants.COMPOSITION_PREFIX) && !name.equals(Constants.MAPPING_SHEET_NAME)) {
                mainNames.add(name);
            }
        }

        // Trova tutte le composizioni che dipendono direttamente dal main (radici)
        for (String mainName : mainNames) {
            List<String> rootCompositions = findCompositionsForParent(mainName);
            for (String rootCompName : rootCompositions) {
                ExcelSheetData rootCompSheet = compositionSheets.get(rootCompName);
                if (rootCompSheet == null) continue;

                // Per ogni riga del foglio composizione radice, genera la mutation update annidata
                List<String> mutations = new ArrayList<>();
                List<String> headers = rootCompSheet.getHeaders();
                String parentHeader = headers.get(0);
                ExternalAttribute rootParentAttr = ExternalAttribute.parseExternalAttribute(parentHeader);

                for (List<String> row : rootCompSheet.getRows()) {
                    // Mappa attributi della composizione radice (salta la colonna parent)
                    Map<String, Object> rootAttributes = new LinkedHashMap<>();
                    String parentValue = row.get(0);
                    for (int i = 1; i < headers.size(); i++) {
                        String header = headers.get(i);
                        String value = (i < row.size()) ? row.get(i) : "";

                        // Se questo campo è parent di una composizione figlia, nidifica la create
                        List<String> childCompositions = findCompositionsForParent(rootCompName.substring(1));
                        for (String childCompName : childCompositions) {
                            ExcelSheetData childSheet = compositionSheets.get(childCompName);
                            if (childSheet != null && header.equals(getCompositionParentHeader(childSheet))) {
                                Object nestedChild = buildNestedComposition(childCompName, rootCompName.substring(1), rootAttributes, value);
                                rootAttributes.put(childCompName.substring(1), nestedChild);
                            }
                        }
                        rootAttributes.put(header, value);
                    }
                    // Genera la mutation di update per la composizione radice, annidando la struttura
                    String mutation = buildUpdateMutationString(
                            rootParentAttr.className, // oggetto main come padre, per update
                            rootParentAttr.attributeName, // attributo parent (es: "nome")
                            parentValue, // valore del parent
                            rootCompName.substring(1), // nome ruolo composizione (senza #)
                            rootAttributes
                    );
                    mutations.add(mutation);
                }
                // La chiave è il nome della composizione radice
                result.put(rootCompName.substring(1), joinMutations(mutations));
            }
        }
        return result;
    }

    private List<String> findCompositionsForParent(String parentClassName) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : compositionParentMap.entrySet()) {
            if (parentClassName.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private Object buildNestedComposition(String compositionSheetName,
                                         String parentClassName,
                                         Map<String, Object> parentAttributes,
                                         String parentValue) {
        ExcelSheetData compositionSheet = compositionSheets.get(compositionSheetName);
        List<Map<String, Object>> compositionObjects = new ArrayList<>();
        List<String> headers = compositionSheet.getHeaders();

        for (List<String> row : compositionSheet.getRows()) {
            String parentColValue = row.get(0);
            if (parentValue != null && !parentColValue.equals(parentValue)) {
                continue;
            }
            if (parentValue == null && !parentAttributes.containsValue(parentColValue)) {
                continue;
            }

            Map<String, Object> compositionRowData = new LinkedHashMap<>();
            for (int i = 1; i < headers.size(); i++) {
                String header = headers.get(i);
                String value = (i < row.size()) ? row.get(i) : "";

                List<String> childCompositions = findCompositionsForParent(compositionSheetName.substring(1));
                for (String childCompName : childCompositions) {
                    ExcelSheetData childSheet = compositionSheets.get(childCompName);
                    if (childSheet != null && header.equals(getCompositionParentHeader(childSheet))) {
                        Object nestedChild = buildNestedComposition(childCompName, compositionSheetName.substring(1), compositionRowData, value);
                        compositionRowData.put(childCompName.substring(1), nestedChild);
                    }
                }
                compositionRowData.put(header, value);
            }
            compositionObjects.add(compositionRowData);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("create", compositionObjects);
        return map;
    }

    private String getCompositionParentHeader(ExcelSheetData sheet) {
        List<String> headers = sheet.getHeaders();
        if (!headers.isEmpty()) {
            return headers.get(0);
        }
        return "";
    }

    private String buildUpdateMutationString(String parentObjectName,
                                            String parentIdentifierAttribute,
                                            String parentIdentifierValue,
                                            String compositionRoleName,
                                            Map<String, Object> compositionAttributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("mutation { ")
                .append(parentObjectName)
                .append(Constants.GRAPHQL_UPDATE_SUFFIX)
                .append("(data: { ")
                .append(parentIdentifierAttribute)
                .append(": \"").append(parentIdentifierValue).append("\", ")
                .append(compositionRoleName)
                .append(": { create: ")
                .append(formatGraphQLMap(compositionAttributes.get("create")))
                .append(" } }) { _id } }");
        return sb.toString();
    }

    private String formatGraphQLMap(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{ ");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(entry.getKey()).append(": ");
                sb.append(formatGraphQLMap(entry.getValue()));
                sb.append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            sb.append(" }");
            return sb.toString();
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[ ");
            for (Object o : list) {
                sb.append(formatGraphQLMap(o)).append(", ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);
            sb.append(" ]");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else {
            return String.valueOf(obj);
        }
    }

    private String joinMutations(List<String> mutations) {
        StringBuilder sb = new StringBuilder();
        for (String m : mutations) {
            sb.append(m).append("\n");
        }
        return sb.toString();
    }
}