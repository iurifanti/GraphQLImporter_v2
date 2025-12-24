package graphql.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Utility class for converting date strings to a consistent format.
 */
public final class DateConverter {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
            // Add more date formats as needed
    };

    /**
     * Attempts to parse a date string into "dd/MM/yyyy" format.
     *
     * @param dateString The date string to parse.
     * @return An Optional containing the formatted date string, or empty if parsing fails.
     */
    public static Optional<String> convertToStandardFormat(String dateString) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateString, formatter);
                return Optional.of(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        return Optional.empty();
    }

    // Private constructor to prevent instantiation
    private DateConverter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}