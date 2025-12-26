package graphql;

import graphql.excel.ExcelDataReader;
import graphql.graphql.GraphQLMutationBuilder;
import graphql.graphql.GraphQLQueryBuilder;
import graphql.graphql.GraphQLService;
import graphql.model.DataFile;
import graphql.model.DataSheet;
import graphql.parser.CompositionParser;
import graphql.parser.MainParser;
import graphql.parser.MutationParser;
import graphql.util.LoggerUI;
import graphql.util.SslBypass;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Punto di ingresso per l'importatore: legge il file Excel e orchestra
 * la generazione ed esecuzione delle mutation verso i due endpoint GraphQL.
 */
public class Application {

    private final ExcelDataReader excelDataReader;
    private final GraphQLMutationBuilder mutationBuilder;
    private final GraphQLQueryBuilder queryBuilder;
    private final GraphQLService graphQLService;
    private final String excelFilePath;

    /**
     * Inizializza tutti i componenti necessari per interpretare il file Excel
     * e comunicare con i servizi GraphQL protetti da autenticazione base.
     */
    public Application(String graphqlEndpoint, String secondaryEndpoint, String username, String password, String excelFilePath) throws Exception {
        this.excelDataReader = new ExcelDataReader();
        this.mutationBuilder = new GraphQLMutationBuilder();
        this.queryBuilder = new GraphQLQueryBuilder();
        // Inizializzazione del servizio con endpoint primario e secondario e credenziali
        this.graphQLService = new GraphQLService(graphqlEndpoint, secondaryEndpoint, username, password);
        this.excelFilePath = excelFilePath;
        SslBypass.disableSslVerificationIfNeeded();
    }

    /**
     * Esegue le mutation di cancellazione per ogni foglio, così da eliminare
     * i dati presenti sul backend prima di eventuali reinserimenti.
     */
    public void delete() throws Exception {
        DataFile fileData = excelDataReader.readExcelFile(excelFilePath);

        for (DataSheet sheetData : fileData.getDataSheets()) {
            LoggerUI.log("\n--- Processing Sheet: " + sheetData.getName() + " ---");
            MutationParser parser = getParserForSheet(sheetData);
            List<String> mutations = parser.parseAndGenerateMutations(sheetData, mutationBuilder, queryBuilder, graphQLService);

            LoggerUI.log("Generated Mutations for sheet '" + sheetData.getName() + "':");
            for (String mutation : mutations) {
                LoggerUI.log(mutation + "\n");
                graphQLService.executeQueryWithFallback(mutation);
            }
        }
    }

    /**
     * Costruisce una mutation di cancellazione bulk per la tabella indicata.
     */
    private static String buildGraphQLDelete(String tableName) {
        String pureName = tableName;
        return "mutation { " + pureName + "___deleteBulk(options: { }) { deleted }}";
    }

    /**
     * Flusso principale di importazione: legge il file Excel, sceglie il parser
     * corretto per ogni foglio e invia le mutation generate, gestendo
     * eventualmente l'ordine inverso per le eliminazioni.
     */
    public void processExcelAndGenerateGraphQL(boolean delete) throws Exception {
        DataFile fileData = excelDataReader.readExcelFile(excelFilePath);

        // Eventuale inversione per cancellazioni, così da rimuovere prima le composizioni
        if (delete) {
            Collections.reverse(fileData.getDataSheets());
        }
        for (DataSheet sheetData : fileData.getDataSheets()) {
            if (delete && sheetData.isComposition()) {
                continue;
            }
            LoggerUI.log("\n--- Processing Sheet: " + sheetData.getName() + " ---");
            MutationParser parser = getParserForSheet(sheetData);
            List<String> mutations;
            if (delete) {
                mutations = Arrays.asList(buildGraphQLDelete(sheetData.getName()));
            } else {
                mutations = parser.parseAndGenerateMutations(sheetData, mutationBuilder, queryBuilder, graphQLService);
            }

            LoggerUI.log("Generated Mutations for sheet '" + sheetData.getName() + "':");
            for (String mutation : mutations) {
                graphQLService.executeQueryWithFallback(mutation);
            }
        }
    }

    /**
     * Seleziona il parser corretto in base al tipo di foglio (main o composizione).
     */
    private MutationParser getParserForSheet(DataSheet sheet) {
        if (sheet.isComposition()) {
            return new CompositionParser();
        } else {
            return new MainParser();
        }
    }

    /**
     * Avvia il processo di importazione o eliminazione gestendo le eccezioni
     * più comuni e notificando l'utente tramite la UI.
     */
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
