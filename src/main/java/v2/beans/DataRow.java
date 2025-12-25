/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author iurif
 */
public class DataRow {
    
    private final List<DataCell> dataCells;

    public DataRow(List<DataCell> dataCells) {
        this.dataCells = Objects.requireNonNull(dataCells, "dataCells");
    }

    public List<DataCell> getDataCells() {
        return dataCells;
    }
}
