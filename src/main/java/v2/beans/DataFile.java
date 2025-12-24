
package v2.beans;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author iurif
 */
public class DataFile {
    private final List<DataSheet> dataSheets;

    public DataFile(List<DataSheet> dataSheets) {
        this.dataSheets = Objects.requireNonNull(dataSheets, "dataSheets");
    }

    public List<DataSheet> getDataSheets() {
        return dataSheets;
    }
}
