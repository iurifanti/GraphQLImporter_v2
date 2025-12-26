package graphql.model;

import common.Utils;
import graphql.util.JsonUtils;
import java.math.BigDecimal;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 * Rappresenta una singola cella del foglio Excel, conservando sia i metadati
 * dell'intestazione sia il valore normalizzato utile per la generazione delle
 * mutation GraphQL.
 */
public class DataCell {

    private final Header header;
    private final Cell cell;
    private final String value;

    /**
     * Costruisce la cella legandola alla relativa intestazione e al valore
     * grezzo estratto dal foglio Excel.
     */
    public DataCell(Header header, Cell cell, String value) {
        this.header = Objects.requireNonNull(header, "header");
        this.cell = cell;
        this.value = value;
    }

    /**
     * Intestazione associata, utile per capire come serializzare il contenuto.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Valore testuale della cella così come letto dal foglio.
     */
    public String getValue() {
        return value;
    }

    /**
     * Controlla se il valore numerico può essere rappresentato come intero
     * senza perdita di informazioni.
     */
    private boolean isInteger() {
        if (!Utils.isBlank(value) && cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            BigDecimal bd = new BigDecimal(value);
            return bd.setScale(0).compareTo(bd) == 0;
        }
        return false;
    }

    /**
     * Verifica se la cella è di tipo booleano nativo in Excel.
     */
    private boolean isBoolean() {
        return cell.getCellType() == CellType.BOOLEAN;
    }

    /**
     * Determina se il valore deve essere racchiuso tra virgolette nella
     * mutation GraphQL.
     */
    public boolean quotationMarksNeeded() {
        return header.isForcedQuotations() || (!isInteger() && !isBoolean());
    }

    /**
     * Restituisce il valore serializzato come stringa pronta per essere
     * inserita nella mutation, gestendo virgolette, numeri e campi vuoti.
     */
    public String getFormattedValue() {
        String formattedValue = value;
        if (isInteger()) {
            formattedValue = new BigDecimal(value).stripTrailingZeros().toPlainString();
        }
        if (quotationMarksNeeded()) {
            formattedValue = "\"" + JsonUtils.escapeJsonString(formattedValue) + "\"";
        }
        if (isBlank()) {
            formattedValue = "";
        }
        return formattedValue;
    }

    /**
     * Indica se la cella non contiene alcun dato significativo.
     */
    public boolean isBlank() {
        return cell.getCellType() == CellType.BLANK || Utils.isBlank(value);
    }

}
