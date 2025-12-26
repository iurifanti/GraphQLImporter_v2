package graphql.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author iurif
 */
public class DataFile {

    private final List<DataSheet> dataSheets;
    public static final String MAPPING_SHEET_NAME = "_mapping";

    public DataFile(List<DataSheet> dataSheets) {
        this.dataSheets = Objects.requireNonNull(dataSheets, "dataSheets");
    }

    public List<DataSheet> getDataSheets() {
        return dataSheets;
    }

    public DataSheet mappingSheet() {
        List<DataSheet> sheets
                = dataSheets.stream().
                        filter(s -> s.getName().equalsIgnoreCase(MAPPING_SHEET_NAME)).
                        collect(Collectors.toList());
        if (sheets.size() == 1) {
            return sheets.get(0);
        } else {
            return null;
        }
    }
}
