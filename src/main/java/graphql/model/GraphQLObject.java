package graphql.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a generic GraphQL object with its name and a map of name2value.
 */
public class GraphQLObject {
    private final String objectName;
    private final Map<String, String> name2value; // Attribute name -> Value
    private final Map<String, String> name2type; // Attribute name -> Type

    public GraphQLObject(String objectName) {
        this.objectName = Objects.requireNonNull(objectName, "Object name cannot be null");
        this.name2value = new LinkedHashMap<>(); // Use LinkedHashMap to preserve insertion order
        this.name2type = new LinkedHashMap<>(); // Use LinkedHashMap to preserve insertion order
    }

    public String getObjectName() {
        return objectName;
    }

    public Map<String, String> getAttributes() {
        return name2value;
    }

    /**
     * Adds an attribute to the GraphQL object.
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void addAttribute(String name, String value) {
        name2value.put(Objects.requireNonNull(name, "Attribute name cannot be null"), value);
    }
    
    public void addAttributeType(String name, String type) {
        name2type.put(Objects.requireNonNull(name, "Attribute name cannot be null"), Objects.requireNonNull(type, "Type cannot be null"));
    }

    @Override
    public String toString() {
        return "GraphQLObject{" +
               "objectName='" + objectName + '\'' +
               ", attributes=" + name2value +
               '}';
    }
}