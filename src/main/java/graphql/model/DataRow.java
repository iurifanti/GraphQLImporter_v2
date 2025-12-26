/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author iurif
 */
public class DataRow {

    private final List<DataCell> dataCells;
    private final Map<Header, DataCell> header2cell = new LinkedHashMap();

    public DataRow(List<DataCell> dataCells) {
        this.dataCells = Objects.requireNonNull(dataCells, "dataCells");
        dataCells.forEach(c -> header2cell.put(c.getHeader(), c));
    }

    public List<DataCell> getDataCells() {
        return dataCells;
    }

    public DataCell get(Header header) {
        return header2cell.get(header);
    }
}
