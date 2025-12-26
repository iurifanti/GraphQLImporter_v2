package graphql.model;

/**
 * Modello per l'intestazione di una colonna del foglio Excel, capace di
 * interpretare prefissi speciali per riferimenti esterni e forzature di
 * formattazione.
 */
public class Header {

    private static final String FORCE_QUOTATION_PREFIX = "§";
    private static final String EXT_REF_PREFIX = "*";
    private final String header;

    // Flag derivato dall'analisi dei valori: se anche un solo valore richiede virgolette, tutte le celle della colonna le useranno
    private boolean inferredQuotations = false;

    /**
     * Inizializza l'intestazione rimuovendo l'eventuale prefisso di
     * riferimento esterno.
     */
    public Header(String header) {
        this.header = header.replace(EXT_REF_PREFIX, "");
    }

    /**
     * Imposta la necessità di usare virgolette per la colonna in base ai dati
     * analizzati.
     */
    public void setInferredQuotations(boolean inferredQuotations) {
        this.inferredQuotations = inferredQuotations;
    }

    /**
     * Restituisce il nome originale dell'intestazione.
     */
    public String getValue() {
        return header;
    }

    /**
     * Indica se la colonna fa riferimento a un'altra tabella (notazione
     * Classe.campo).
     */
    public boolean isReference() {
        return header.contains(".");
    }

    private String defaultRoleName() {
        return decapitalize(getReferencedClassName()) + "_";
    }

    /**
     * Segnala se le virgolette sono state dedotte dai valori letti.
     */
    public boolean isInferredQuotations() {
        return inferredQuotations;
    }

    /**
     * Controlla se il prefisso forza l'uso delle virgolette.
     */
    public boolean isForcedQuotations() {
        return header.startsWith(FORCE_QUOTATION_PREFIX);
    }

    /**
     * Ricava il nome del ruolo per una composizione o riferimento esterno,
     * eventualmente personalizzato tra parentesi quadre.
     */
    public String getReferencedRoleName() {
        if (!isReference()) {
            return null;
        }

        if (header.contains("[")) {
            int start = header.indexOf('[') + 1;
            int end = header.indexOf(']', start);
            if (end > start) {
                return header.substring(start, end);
            }
        }
        return defaultRoleName();
    }

    /**
     * Restituisce il nome della classe referenziata nella colonna.
     */
    public String getReferencedClassName() {
        if (isReference()) {
            return header.replace(EXT_REF_PREFIX, "").split("\\.")[0].split("\\[")[0];
        } else {
            return null;
        }
    }

    /**
     * Restituisce il nome dell'attributo referenziato nella classe esterna.
     */
    public String getReferencedAttributeName() {
        if (isReference()) {
            return header.split("\\.")[1];
        } else {
            return null;
        }
    }

    private static String decapitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    public String getAttributeName() {
        if (isReference()) {
            return getReferencedRoleName();
        } else {
            return header.replace(FORCE_QUOTATION_PREFIX, "");
        }
    }

}
