package graphql.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Modello che incapsula un foglio del file Excel, con nome, intestazioni e
 * tutte le righe di dati utili per costruire le mutation GraphQL.
 */
public class DataSheet {

    private String name;
    private final List<Header> headers;
    private final List<DataRow> dataRows;

    private static final String COMPOSITION_PREFIX = "#";

    /**
     * Crea la rappresentazione del foglio con nome, intestazioni e righe già
     * normalizzati dal reader Excel.
     */
    public DataSheet(String name, List<Header> headers, List<DataRow> dataRows) {
        this.name = Objects.requireNonNull(name, "name");
        this.headers = Objects.requireNonNull(headers, "headers");
        this.dataRows = Objects.requireNonNull(dataRows, "dataRows");
    }

    /**
     * Nome originale del foglio, comprensivo di eventuale prefisso per le
     * composizioni.
     */
    public String getName() {
        return name;
    }

    /**
     * Aggiorna il nome del foglio, utile quando si applicano mapping
     * personalizzati.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Restituisce la lista di intestazioni in ordine di apparizione.
     */
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * Restituisce le righe lette dal foglio.
     */
    public List<DataRow> getDataRows() {
        return dataRows;
    }

    /**
     * Indica se il foglio rappresenta una composizione (prefisso '#').
     */
    public boolean isComposition() {
        return name.startsWith(COMPOSITION_PREFIX);
    }

    /**
     * Calcola il nome del ruolo della composizione senza prefisso.
     */
    public String getCompositionRoleName() {
        return name.replace(COMPOSITION_PREFIX, "");
    }

    /**
     * Restituisce solo le intestazioni che rappresentano riferimenti esterni.
     */
    public List<Header> externalHeaders() {
        return headers.stream().filter(Header::isReference).collect(Collectors.toList());
    }
}
