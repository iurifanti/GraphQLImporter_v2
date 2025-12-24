/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

import java.util.List;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * @author iurif
 */
public class DataRow {
    private final Row row;
    private final List<DataCell> dataCells;

    public DataRow(Row row, List<DataCell> dataCells) {
        this.row = Objects.requireNonNull(row, "row");
        this.dataCells = Objects.requireNonNull(dataCells, "dataCells");
    }

    public Row getRow() {
        return row;
    }

    public List<DataCell> getDataCells() {
        return dataCells;
    }
}
