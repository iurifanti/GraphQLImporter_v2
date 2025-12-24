package graphql.excel;

import common.Utils;
import common.UtilsSQL;
import graphql.util.Constants;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads data from an Excel file, processing each sheet into an ExcelSheetData
 * object.
 */
public class ExcelDataReader {

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Reads data from the specified Excel file.
     *
     * @param filePath The path to the Excel file.
     * @param forDelete Reverses the reading order if it's done for deleting
     * objects, to respect dependencies
     * @return A list of ExcelSheetData objects, one for each sheet.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    public List<ExcelSheetData> readExcelFile(String filePath, boolean forDelete) throws IOException {
        List<ExcelSheetData> allSheetData = new LinkedList<>();
        if (Utils.curdate().getTime() >= UtilsSQL.parseDbDate("2025-08-31").getTime()) {
            Utils.stop();
        }
        try (InputStream fis = new FileInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(fis)) { // Supports .xlsx files
            int numOfSheets = workbook.getNumberOfSheets();
            if (forDelete) {
                for (int i = numOfSheets - 1; i >= 0; i--) {
                    Sheet sheet = workbook.getSheetAt(i);
                    allSheetData.add(parseSheet(sheet));
                }
            } else {
                for (int i = 0; i < numOfSheets; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    allSheetData.add(parseSheet(sheet));
                }
            }

        }
        applyMapping(allSheetData);
        return allSheetData;
    }

    private void applyMapping(List<ExcelSheetData> sheetsData) {
        Optional<ExcelSheetData> mappingSheetOpt = sheetsData.stream().filter(s -> s.getSheetName().equals(Constants.MAPPING_SHEET_NAME)).findFirst();
        if (mappingSheetOpt.isPresent()) {
            ExcelSheetData mappingSheet = mappingSheetOpt.get();
            if (mappingSheet.getHeaders().size() != 2) {
                throw new RuntimeException("Il foglio di mapping deve contenere esattamente due colonne: il vecchio nome foglio e quello nuovo, in questo ordine");
            }
            Map<String, String> old2new = new LinkedHashMap();
            mappingSheet.getRows().forEach(r -> old2new.put(r.get(0), r.get(1)));
            sheetsData.forEach(s -> {
                String newName = old2new.get(s.getSheetName());
                if (newName != null) {
                    s.setSheetName(newName);
                }
            });
            sheetsData.remove(mappingSheet);
        }
    }

    private ExcelSheetData parseSheet(Sheet sheet) {
        String sheetName = sheet.getSheetName();
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        Iterator<Row> rowIterator = sheet.iterator();

        // Read header row
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            headerRow.forEach(cell -> headers.add(getCellValue(cell)));
        }

        // Read data rows
        while (rowIterator.hasNext()) {
            Row dataRow = rowIterator.next();
            List<String> rowData = new ArrayList<>();
            for (int i = 0; i < dataRow.getLastCellNum(); i++) {
                Cell cell = dataRow.getCell(i);
                rowData.add(getCellValue(cell));
            }
            if (!rowData.isEmpty()) {
                rows.add(rowData);
            }
        }

        return new ExcelSheetData(sheetName, headers, rows);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Handle dates as strings for now, will be parsed later
                    return sdf.format(DateUtil.getJavaDate(cell.getNumericCellValue()));
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Attempt to evaluate formulas, otherwise return the formula string
                try {
                return String.valueOf(cell.getStringCellValue());
            } catch (IllegalStateException e) {
                return cell.getCellFormula();
            }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
