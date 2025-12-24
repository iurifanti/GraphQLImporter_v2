/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Sheet;

/**
 *
 * @author iurif
 */
public class DataSheet {
    private final Sheet sheet;
    private final String name;
    private final List<Header> headers;
    private final List<DataRow> dataRows;

    public DataSheet(Sheet sheet, String name, List<Header> headers, List<DataRow> dataRows) {
        this.sheet = Objects.requireNonNull(sheet, "sheet");
        this.name = Objects.requireNonNull(name, "name");
        this.headers = Objects.requireNonNull(headers, "headers");
        this.dataRows = Objects.requireNonNull(dataRows, "dataRows");
    }

    public Sheet getSheet() {
        return sheet;
    }

    public String getName() {
        return name;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public List<DataRow> getDataRows() {
        return dataRows;
    }
}
