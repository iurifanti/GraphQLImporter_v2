package graphql.model;

/**
 * Enum representing the expected data types for GraphQL fields.
 */
public enum GQLFieldType {
    STRING,
    DECIMAL,
    DATE,
    INTEGER,
    ID // Special type for references to external IDs
}