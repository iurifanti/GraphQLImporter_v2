package graphql.excel;

import graphql.model.DataCell;
import graphql.model.DataFile;
import graphql.model.DataRow;
import graphql.model.DataSheet;
import graphql.model.Header;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reader specializzato che trasforma un file Excel in strutture dati
 * utilizzabili dai parser GraphQL.
 */
public class ExcelDataReader {

    private final DataFormatter dataFormatter = new DataFormatter();

    /**
     * Carica il file Excel e converte ogni foglio in un {@link DataSheet}.
     */
    public DataFile readExcelFile(String filePath) throws IOException {
        try (InputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<DataSheet> sheets = new ArrayList<>();
            for (Sheet sheet : workbook) {
                sheets.add(parseSheet(sheet, evaluator));
            }
            DataFile data = new DataFile(sheets);
            fixMappingSheet(data);
            return data;
        }
    }

    /**
     * Converte un singolo foglio in intestazioni e righe di dati già
     * formattati.
     */
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
                        String value = readCellAsString(cell, evaluator);
                        DataCell dataCell = new DataCell(header, cell, value);
                        cells.add(dataCell);
                        if (!header.isForcedQuotations() && !header.isInferredQuotations()) {
                            header.setInferredQuotations(dataCell.quotationMarksNeeded());
                        }
                    }
                }
                dataRows.add(new DataRow(cells));
            }
        }

        return new DataSheet(sheet.getSheetName(), headers, dataRows);
    }

    /**
     * Ritorna il contenuto della cella in formato stringa, gestendo le
     * eventuali formule tramite l'evaluator.
     */
    private String readCellAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell, evaluator);
    }

    /**
     * Applica il foglio speciale "_mapping" per rinominare i fogli seguendo le
     * regole definite dall'utente.
     */
    private void fixMappingSheet(DataFile data) {
        DataSheet mappingSheet = data.mappingSheet();
        if (mappingSheet == null) {
            return;
        }
        data.getDataSheets().remove(mappingSheet);
        if (mappingSheet.getHeaders().size() != 2) {
            throw new RuntimeException("Il foglio di mapping deve contenere esattamente due colonne");
        }
        Map<String, String> oldName2newName = new LinkedHashMap();
        mappingSheet.getDataRows().forEach(r -> oldName2newName.put(r.get(0).getValue(), r.get(1).getValue()));
        data.getDataSheets().stream().
                filter(s -> oldName2newName.keySet().contains(s.getName())).
                forEach(s -> s.setName(oldName2newName.get(s.getName())));
    }

    public static void main(String[] args) throws Exception {
        ExcelDataReader r = new ExcelDataReader();
        r.readExcelFile("C:\\Users\\iurif\\Desktop\\dropout.xlsx").getDataSheets().get(0).getDataRows().forEach(dr -> dr.getDataCells().forEach(c -> System.out.println(c.getFormattedValue())));
    }
}
