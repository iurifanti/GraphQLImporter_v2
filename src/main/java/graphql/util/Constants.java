package graphql.util;

/**
 * Utility class for constants used throughout the application.
 */
public final class Constants {

    public static final String COMPOSITION_PREFIX = "#";
    public static final String DEPENDENT_SEPARATOR = ".";
    public static final String REAL_PREFIX = "§";
    public static final String GRAPHQL_CREATE_SUFFIX = "___create";
    public static final String GRAPHQL_UPDATE_SUFFIX = "___update";
    public static final String GRAPHQL_GET_BY_PREFIX = "___getBy"; // Used for fetching ID for dependent objects
    public static final String MAPPING_SHEET_NAME = "_mapping";
    public static final String ID = "_id";
    

    // Private constructor to prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}