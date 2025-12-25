package v2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import v2.beans.DataCell;
import v2.beans.DataFile;
import v2.beans.DataRow;
import v2.beans.DataSheet;
import v2.beans.Header;

public class ExcelBeansReader {

    private final DataFormatter dataFormatter = new DataFormatter();

    public DataFile readExcelFile(String filePath) throws IOException {
        try (InputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<DataSheet> sheets = new ArrayList<>();
            for (Sheet sheet : workbook) {
                sheets.add(parseSheet(sheet, evaluator));
            }
            return new DataFile(sheets);
        }
    }

    private DataSheet parseSheet(Sheet sheet, FormulaEvaluator evaluator) {
        List<Header> headers = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();

        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            headerRow.forEach(cell -> headers.add(new Header(readCellAsString(cell, evaluator))));
        }

        List<DataRow> dataRows = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row != null) {
                List<DataCell> cells = new ArrayList<>();
                short lastCellNum = row.getLastCellNum();
                int totalCells = Math.max(lastCellNum, headers.size());
                for (int i = 0; i < totalCells; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        Header header = i < headers.size() ? headers.get(i) : new Header("");
                        cells.add(new DataCell(header, dataFormatter.formatCellValue(cell, evaluator)));
                    }
                }
                dataRows.add(new DataRow(cells));
            }
        }

        return new DataSheet(sheet.getSheetName(), headers, dataRows);
    }

    private String readCellAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell, evaluator);
    }

    public static void main(String[] args) throws Exception {
        ExcelBeansReader r = new ExcelBeansReader();
        r.readExcelFile("C:\\Users\\iurif\\Desktop\\dropout.xlsx").getDataSheets().get(0).getDataRows().forEach(dr -> dr.getDataCells().forEach(c -> System.out.println(c.getValue())));
    }
}
