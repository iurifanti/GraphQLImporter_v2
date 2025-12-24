/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package v2.beans;

import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;

/**
 *
 * @author iurif
 */
public class DataSheet {
    private Sheet sheet;
    private String name;
    private List<Header> headers;
    private List<DataRow> dataRows;
}
