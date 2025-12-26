/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphql.model;

import common.Utils;
import graphql.util.JsonUtils;
import java.math.BigDecimal;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

/**
 *
 * @author iurif
 */
public class DataCell {

    private final Header header;
    private final Cell cell;
    private final String value;

    public DataCell(Header header, Cell cell, String value) {
        this.header = Objects.requireNonNull(header, "header");
        this.cell = cell;
        this.value = value;
    }

    public Header getHeader() {
        return header;
    }

    public String getValue() {
        return value;
    }

    private boolean isInteger() {
        if (!Utils.isBlank(value) && cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            BigDecimal bd = new BigDecimal(value);
            return bd.setScale(0).compareTo(bd) == 0;
        }
        return false;
    }

    private boolean isBoolean() {
        return cell.getCellType() == CellType.BOOLEAN;
    }

    public boolean quotationMarksNeeded() {
        return header.isForcedQuotations() || (!isInteger() && !isBoolean());
    }

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

    public boolean isBlank() {
        return cell.getCellType() == CellType.BLANK || Utils.isBlank(value);
    }

}
