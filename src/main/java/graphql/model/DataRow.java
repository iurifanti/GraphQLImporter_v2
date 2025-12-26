package graphql.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Modello immutabile che rappresenta una riga di dati, mantenendo sia
 * l'ordine originale delle celle sia una mappatura rapida per accedervi
 * tramite l'intestazione.
 */
public class DataRow {

    private final List<DataCell> dataCells;
    private final Map<Header, DataCell> header2cell = new LinkedHashMap();

    /**
     * Costruisce la riga e prepara la mappa di lookup per le celle.
     */
    public DataRow(List<DataCell> dataCells) {
        this.dataCells = Objects.requireNonNull(dataCells, "dataCells");
        dataCells.forEach(c -> header2cell.put(c.getHeader(), c));
    }

    /**
     * Restituisce l'elenco ordinato delle celle presenti nella riga.
     */
    public List<DataCell> getDataCells() {
        return dataCells;
    }

    /**
     * Recupera la cella associata a una specifica intestazione.
     */
    public DataCell get(Header header) {
        return header2cell.get(header);
    }

    /**
     * Recupera la cella in base all'indice originale nel foglio.
     */
    public DataCell get(int index) {
        return dataCells.get(index);
    }
}
