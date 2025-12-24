package graphql.excel;

import java.util.List;
import java.util.Objects;

/**
 * Represents the data extracted from a single Excel sheet.
 */
public class ExcelSheetData {

    private String sheetName;
    private final List<String> headers;
    private final List<List<String>> rows; // Each inner list is a row of data

    public ExcelSheetData(String sheetName, List<String> headers, List<List<String>> rows) {
        this.sheetName = Objects.requireNonNull(sheetName, "Sheet name cannot be null");
        this.headers = Objects.requireNonNull(headers, "Headers cannot be null");
        this.rows = Objects.requireNonNull(rows, "Rows cannot be null");
    }

    public String getSheetName() {
        return sheetName;
    }

    public String setSheetName(String sheetName) {
        return this.sheetName = sheetName;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    @Override
    public String toString() {
        return "ExcelSheetData{"
                + "sheetName='" + sheetName + '\''
                + ", headers=" + headers
                + ", rows=" + rows
                + '}';
    }
}
