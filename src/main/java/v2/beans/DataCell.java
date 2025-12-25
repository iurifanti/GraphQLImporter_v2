/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

import java.util.Objects;

/**
 *
 * @author iurif
 */
public class DataCell {

    private final Header header;
    private final String value;

    public DataCell(Header header, String value) {
        this.header = Objects.requireNonNull(header, "header");
        this.value = value;
    }

    public Header getHeader() {
        return header;
    }

    public String getValue() {
        return value;
    }
    
}
