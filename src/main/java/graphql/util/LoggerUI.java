package graphql.util;

import graphql.GraphQLExcelForm;

/**
 * Logger minimale che inoltra i messaggi verso la UI, esposto come singleton
 * per essere accessibile dai vari componenti applicativi.
 */
public class LoggerUI {

    GraphQLExcelForm form;

    private static LoggerUI inst = null;

    private LoggerUI(GraphQLExcelForm form) {
        this.form = form;
    }

    /**
     * Inizializza l'istanza condivisa da usare in tutto il ciclo di vita
     * dell'applicazione.
     */
    public static void createInstance(GraphQLExcelForm form) {
        inst = new LoggerUI(form);
    }

    /**
     * Registra un messaggio informativo nel log grafico se disponibile.
     */
    public static void log(Object obj) {
        if (obj != null) {
            inst.form.logInfo(obj.toString());
        }
    }
}
