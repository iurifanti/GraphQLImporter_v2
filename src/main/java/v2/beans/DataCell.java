/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

import org.apache.poi.ss.usermodel.Cell;
import java.util.Objects;

/**
 *
 * @author iurif
 */
public class DataCell {
    private final Header header;
    private final Cell cell;

    public DataCell(Header header, Cell cell) {
        this.header = Objects.requireNonNull(header, "header");
        this.cell = cell;
    }

    public Header getHeader() {
        return header;
    }

    public Cell getCell() {
        return cell;
    }
}
