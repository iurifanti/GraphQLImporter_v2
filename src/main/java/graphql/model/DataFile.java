package graphql.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Rappresenta l'intero file Excel, mantenendo l'elenco dei fogli e
 * fornendo utilità per individuare eventuali schede di mapping.
 */
public class DataFile {

    private final List<DataSheet> dataSheets;
    public static final String MAPPING_SHEET_NAME = "_mapping";

    /**
     * Inizializza il contenitore dei fogli letti dal workbook Excel.
     */
    public DataFile(List<DataSheet> dataSheets) {
        this.dataSheets = Objects.requireNonNull(dataSheets, "dataSheets");
    }

    /**
     * Restituisce tutti i fogli presenti.
     */
    public List<DataSheet> getDataSheets() {
        return dataSheets;
    }

    /**
     * Recupera il foglio speciale "_mapping" se presente, altrimenti null.
     */
    public DataSheet mappingSheet() {
        List<DataSheet> sheets
                = dataSheets.stream().
                        filter(s -> s.getName().equalsIgnoreCase(MAPPING_SHEET_NAME)).
                        collect(Collectors.toList());
        if (sheets.size() == 1) {
            return sheets.get(0);
        } else {
            return null;
        }
    }
}
