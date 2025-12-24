package graphql;

import graphql.excel.ExcelDataReader;
import graphql.excel.ExcelSheetData;
import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.parser.CompositionParser;
import graphql.parser.DataParser;
import graphql.parser.MainDependentParser;
import graphql.util.Constants;
import graphql.util.LoggerUI;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

public class Application {

    private final ExcelDataReader excelDataReader;
    private final GraphQLMutationBuilder mutationBuilder;
    private final GraphQLQueryBuilder queryBuilder;
    private final GraphQLService graphQLService;
    private final String excelFilePath;

    public Application(String graphqlEndpoint, String secondaryEndpoint, String username, String password, String excelFilePath) throws Exception {
        this.excelDataReader = new ExcelDataReader();
        this.mutationBuilder = new GraphQLMutationBuilder();
        this.queryBuilder = new GraphQLQueryBuilder();
        // Usa il costruttore di GraphQLService che supporta due endpoint e autenticazione base
        this.graphQLService = new GraphQLService(graphqlEndpoint, secondaryEndpoint, username, password);
        this.excelFilePath = excelFilePath;
    }

    public void delete() throws Exception {
        List<ExcelSheetData> sheetsData = excelDataReader.readExcelFile(excelFilePath, true);

        for (ExcelSheetData sheetData : sheetsData) {
            LoggerUI.log("\n--- Processing Sheet: " + sheetData.getSheetName() + " ---");
            DataParser parser = getParserForSheet(sheetData.getSheetName());
            List<String> mutations = parser.parseAndGenerateMutations(sheetData, mutationBuilder, queryBuilder, graphQLService);

            LoggerUI.log("Generated Mutations for sheet '" + sheetData.getSheetName() + "':");
            for (String mutation : mutations) {
                LoggerUI.log(mutation + "\n");
                graphQLService.executeQueryWithFallback(mutation);
            }
        }
    }

    private static String buildGraphQLDelete(String tableName) {
        String pureName = tableName;
        return "mutation { " + pureName + "___deleteBulk(options: { }) { deleted }}";
    }

    public void processExcelAndGenerateGraphQL(boolean delete) throws Exception {
        List<ExcelSheetData> sheetsData = excelDataReader.readExcelFile(excelFilePath, delete);
        
        /********/
//        NestedCompositionUpdateBuilder t = new NestedCompositionUpdateBuilder(sheetsData);
//        LoggerUI.log("### TEST ###");
//        LoggerUI.log(t.buildNestedCompositionsUpdateMutations(sheetsData));
        LoggerUI.log("############");
        /********/
        
        for (ExcelSheetData sheetData : sheetsData) {
            if (delete && sheetData.getSheetName().startsWith(Constants.COMPOSITION_PREFIX)) {
                continue;
            }
            LoggerUI.log("\n--- Processing Sheet: " + sheetData.getSheetName() + " ---");
            DataParser parser = getParserForSheet(sheetData.getSheetName());
            List<String> mutations;
            if (delete) {
                mutations = Arrays.asList(buildGraphQLDelete(sheetData.getSheetName()));
            } else {
                mutations = parser.parseAndGenerateMutations(sheetData, mutationBuilder, queryBuilder, graphQLService);
            }

            LoggerUI.log("Generated Mutations for sheet '" + sheetData.getSheetName() + "':");
            for (String mutation : mutations) {
                graphQLService.executeQueryWithFallback(mutation);
            }
        }
    }

    private DataParser getParserForSheet(String sheetName) {
        if (sheetName.startsWith(Constants.COMPOSITION_PREFIX)) {
            return new CompositionParser();
        } else {
            return new MainDependentParser();
        }
    }
    
    public static void run(String graphqlEndpoint, String secondaryEndpoint, String username, String password, String excelFilePath, boolean delete) {
        try {
            Application app = new Application(graphqlEndpoint, secondaryEndpoint, username, password, excelFilePath);
            app.processExcelAndGenerateGraphQL(delete);
            LoggerUI.log("\n--- Processing complete. ---");
        } catch (IOException e) {
            LoggerUI.log("Error during network communication or JSON processing: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LoggerUI.log("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GraphQLExcelForm form = new GraphQLExcelForm();
            LoggerUI.createInstance(form);
            form.setVisible(true);
            form.logInfo("Applicazione pronta.");
        });
    }
}
