package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschematypes.PathUtils.append;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class ObjectSchema extends Schema {
  private final JSONObject schemaJson; // Kept for debugging only.
  private final Map<String, Schema> _properties = new HashMap<>();
  private final Collection<Ecma262Pattern> patternPropertiesPatterns = new ArrayList<>();
  private final Collection<Schema> patternPropertiesSchemas = new ArrayList<>();
  private final Set<String> required = new HashSet<>();
  private final List<Schema> itemsArray = new ArrayList<>();
  private final Schema itemsSingle;
  private final int minItems;
  private final int maxItems;
  private final Collection<Schema> allOf;
  private final Collection<Schema> anyOf;
  private final Collection<Schema> oneOf;
  private final Set<String> explicitTypes;
  private final double minimum;
  private final double maximum;
  private final Double exclusiveMinimum;
  private final Double exclusiveMaximum;
  private final Double multipleOf;
  private final int minLength;
  private final int maxLength;
  private final int minProperties;
  private final Schema additionalProperties;
  private final Schema additionalItems;
  private final Map<String, Schema> definitions = new HashMap<>();
  private final Object _const;
  private final Set<Object> _enum;
  private final Schema contains;
  private final Schema _if;
  private final Schema _then;
  private final Schema _else;
  private final Map<String, Collection<String>> dependencies = new HashMap<>();
  private final Map<String, Schema> schemaDependencies = new HashMap<>();

  public ObjectSchema(SchemaStore schemaStore, URI path) throws GenerationException {
    super(schemaStore, path);
    JSONObject jsonObject = (JSONObject) schemaStore.getSchemaJson(path);
    this.schemaJson = jsonObject;
    if (jsonObject == null) {
      throw new GenerationException("Could not obtain " + path);
    }

    // Get explicit types.
    Object type = jsonObject.opt("type");
    if (type instanceof JSONArray) {
      explicitTypes = new HashSet<>();
      JSONArray array = (JSONArray) type;
      for (int idx = 0; idx != array.length(); idx++) {
        explicitTypes.add(array.getString(idx));
      }
    } else if (type instanceof String) {
      explicitTypes = Set.of(type.toString());
    } else {
      explicitTypes = null;
    }

    // Get properties.
    if (jsonObject.has("properties")) {
      JSONObject properties = jsonObject.getJSONObject("properties");
      URI propertiesPointer = append(path, "properties");
      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        _properties.put(
            propertyName, schemaStore.getSchema(append(propertiesPointer, propertyName)));
      }
    }

    if (jsonObject.has("patternProperties")) {
      JSONObject patternProperties = jsonObject.getJSONObject("patternProperties");
      URI propertiesPointer = append(path, "patternProperties");
      Iterator<String> it = patternProperties.keys();
      while (it.hasNext()) {
        String propertyPattern = it.next();
        patternPropertiesPatterns.add(new Ecma262Pattern(propertyPattern));
        patternPropertiesSchemas.add(
            schemaStore.getSchema(append(propertiesPointer, propertyPattern)));
      }
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.2.3
    if (jsonObject.has("additionalProperties")) {
      additionalProperties = schemaStore.getSchema(append(path, "additionalProperties"));
    } else {
      additionalProperties = null;
    }

    if (jsonObject.has("additionalItems")) {
      additionalItems = schemaStore.getSchema(append(path, "additionalItems"));
    } else {
      additionalItems = null;
    }

    if (jsonObject.has("definitions")) {
      URI definitionsPointer = append(path, "definitions");
      JSONObject definitionsObject = jsonObject.getJSONObject("definitions");
      for (String definition : definitionsObject.keySet()) {
        definitions.put(definition, schemaStore.getSchema(append(definitionsPointer, definition)));
      }
    }

    if (jsonObject.has("required")) {
      JSONArray array = jsonObject.getJSONArray("required");
      for (int idx = 0; idx != array.length(); idx++) {
        required.add(array.getString(idx));
      }
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.1
    Object items = jsonObject.opt("items");
    URI itemsPath = append(path, "items");
    if (items instanceof JSONObject || items instanceof Boolean) {
      itemsSingle = schemaStore.getSchema(itemsPath);
    } else {
      itemsSingle = null;
      if (items instanceof JSONArray) {
        JSONArray jsonArray = (JSONArray) items;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          itemsArray.add(schemaStore.getSchema(append(itemsPath, String.valueOf(idx))));
        }
      }
    }

    minItems = jsonObject.optInt("minItems", 0);
    maxItems = jsonObject.optInt("maxItems", Integer.MAX_VALUE);

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.4
    if (jsonObject.has("contains")) {
      contains = schemaStore.getSchema(append(path, "contains"));
    } else {
      contains = null;
    }

    if (jsonObject.has("if")) {
      _if = schemaStore.getSchema(append(path, "if"));
    } else {
      _if = null;
    }

    if (jsonObject.has("then")) {
      _then = schemaStore.getSchema(append(path, "then"));
    } else {
      _then = null;
    }

    if (jsonObject.has("else")) {
      _else = schemaStore.getSchema(append(path, "else"));
    } else {
      _else = null;
    }

    if (jsonObject.has("allOf")) {
      allOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("allOf");
      URI arrayPath = append(path, "allOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(schemaStore.getSchema(indexPointer));
      }
    } else {
      allOf = null;
    }

    if (jsonObject.has("anyOf")) {
      anyOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("anyOf");
      URI arrayPath = append(path, "anyOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        anyOf.add(schemaStore.getSchema(indexPointer));
      }
    } else {
      anyOf = null;
    }

    if (jsonObject.has("oneOf")) {
      oneOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("oneOf");
      URI arrayPath = append(path, "oneOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        oneOf.add(schemaStore.getSchema(indexPointer));
      }
    } else {
      oneOf = null;
    }

    minimum = jsonObject.optDouble("minimum", -Double.MAX_VALUE);

    maximum = jsonObject.optDouble("maximum", Double.MAX_VALUE);

    if (jsonObject.has("exclusiveMinimum")) {
      exclusiveMinimum = jsonObject.getDouble("exclusiveMinimum");
    } else {
      exclusiveMinimum = null;
    }

    if (jsonObject.has("exclusiveMaximum")) {
      exclusiveMaximum = jsonObject.getDouble("exclusiveMaximum");
    } else {
      exclusiveMaximum = null;
    }

    if (jsonObject.has("multipleOf")) {
      multipleOf = jsonObject.getDouble("multipleOf");
    } else {
      multipleOf = null;
    }

    minLength = jsonObject.optInt("minLength", 0);
    maxLength = jsonObject.optInt("maxLength", Integer.MAX_VALUE);
    minProperties = jsonObject.optInt("minProperties", 0);
    _const = jsonObject.opt("const");

    if (jsonObject.has("enum")) {
      _enum = new HashSet<>();
      JSONArray enumArray = jsonObject.getJSONArray("enum");
      for (int idx = 0; idx != enumArray.length(); idx++) {
        _enum.add(enumArray.get(idx));
      }
    } else {
      _enum = null;
    }

    if (jsonObject.has("dependencies")) {
      JSONObject dependenciesObject = jsonObject.getJSONObject("dependencies");
      for (String dependency : dependenciesObject.keySet()) {
        List<String> spec = new ArrayList<>();
        Object dependencyObject = dependenciesObject.get(dependency);
        if (dependencyObject instanceof JSONArray) {
          JSONArray array = (JSONArray) dependencyObject;
          for (int idx = 0; idx != array.length(); idx++) {
            spec.add(array.getString(idx));
          }
          dependencies.put(dependency, spec);
        } else {
          URI dependenciesPpinter = append(path, "dependencies");
          schemaDependencies.put(
              dependency, schemaStore.getSchema(append(dependenciesPpinter, dependency)));
        }
      }
    }
  }

  private static boolean compare(Object a, Object b) {
    if (a instanceof Integer && b instanceof Integer) {
      return a.equals(b);
    } else if (a instanceof Number && b instanceof Number) {
      return ((Number) a).doubleValue() == ((Number) b).doubleValue();
    } else if ((a instanceof JSONObject && b instanceof JSONObject)
        || (a instanceof JSONArray && b instanceof JSONArray)) {
      return a.toString().equals(b.toString());
    } else {
      return a.equals(b);
    }
  }

  @Override
  public void validate(Object document, URI path, Consumer<ValidationError> errorConsumer) {
    Object object = PathUtils.objectAtPath(document, path);
    if (object instanceof Number) {
      if (object instanceof Integer) {
        typeCheck(Set.of("integer", "number"), document, path, errorConsumer);
      } else {
        typeCheck(Set.of("number"), document, path, errorConsumer);
      }
      Number number = (Number) object;

      if (number.doubleValue() < minimum) {
        errorConsumer.accept(error(document, path, "Less than minimum"));
      }

      if (exclusiveMinimum != null) {
        if (number.doubleValue() <= exclusiveMinimum) {
          errorConsumer.accept(error(document, path, "Less than or equal to exclusive minimum"));
        }
      }

      if (number.doubleValue() > maximum) {
        errorConsumer.accept(error(document, path, "Greater than maximum"));
      }

      if (exclusiveMaximum != null) {
        if (number.doubleValue() >= exclusiveMaximum) {
          errorConsumer.accept(error(document, path, "Greater than or equal to exclusive maximum"));
        }
      }
      if (multipleOf != null) {
        if (number.doubleValue() / multipleOf % 1 != 0) {
          errorConsumer.accept(error(document, path, "Not a multiple"));
        }
      }
    } else if (object instanceof Boolean) {
      typeCheck(Set.of("boolean"), document, path, errorConsumer);
    } else if (object instanceof String) {
      typeCheck(Set.of("string"), document, path, errorConsumer);
      String string = (String) object;

      if (string.length() < minLength) {
        errorConsumer.accept(error(document, path, "Shorter than minLength"));
      }

      if (string.length() > maxLength) {
        errorConsumer.accept(error(document, path, "Longer than maxLength"));
      }
    } else if (object instanceof JSONArray) {
      typeCheck(Set.of("array"), document, path, errorConsumer);

      JSONArray jsonArray = (JSONArray) object;
      if (jsonArray.length() < minItems) {
        errorConsumer.accept(error(document, path, "Below min items"));
      }

      if (jsonArray.length() > maxItems) {
        errorConsumer.accept(error(document, path, "Above max length"));
      }

      if (itemsSingle != null) {
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          itemsSingle.validate(document, append(path, String.valueOf(idx)), errorConsumer);
        }
      }

      if (!itemsArray.isEmpty()) {
        if (jsonArray.length() > itemsArray.size() && additionalItems != null) {
          for (int idx = itemsArray.size(); idx != jsonArray.length(); idx++) {
            additionalItems.validate(document, append(path, String.valueOf(idx)), errorConsumer);
          }
        }
        for (int idx = 0; idx != Math.min(itemsArray.size(), jsonArray.length()); idx++) {
          itemsArray.get(idx).validate(document, append(path, String.valueOf(idx)), errorConsumer);
        }
      }

      if (contains != null) {
        boolean onePassed = false;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          List<ValidationError> errors = new ArrayList<>();
          contains.validate(document, append(path, String.valueOf(idx)), errors::add);
          if (errors.isEmpty()) {
            onePassed = true;
            break;
          }
        }
        if (!onePassed) {
          errorConsumer.accept(error(document, path, "No element in the array matched contains"));
        }
      }
    } else if (object instanceof JSONObject) {
      typeCheck(Set.of("object"), document, path, errorConsumer);
      JSONObject jsonObject = (JSONObject) object;
      if (jsonObject.length() < minProperties) {
        errorConsumer.accept(error(document, path, "Too few properties"));
      }
      Set<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          Schema schema = _properties.get(property);
          schema.validate(document, append(path, property), errorConsumer);
          remainingProperties.remove(property);
        }
        Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchemas.iterator();
        while (it0.hasNext()) {
          Ecma262Pattern pattern = it0.next();
          Schema schema = it1.next();
          if (pattern.matches(property)) {
            schema.validate(document, append(path, property), errorConsumer);
            remainingProperties.remove(property);
          }
        }
      }
      if (additionalProperties != null) {
        for (String property : remainingProperties) {
          additionalProperties.validate(document, append(path, property), errorConsumer);
        }
      }

      for (String property : required) {
        if (!jsonObject.has(property)) {
          errorConsumer.accept(error(document, path, "Missing required property " + property));
        }
      }

      for (Map.Entry<String, Collection<String>> entry : dependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }

        Collection<String> _dependencies = entry.getValue();
        for (String dependency : _dependencies) {
          if (jsonObject.has(dependency)) {
            continue;
          }
          errorConsumer.accept(
              error(document, path, "Missing dependency " + property + " -> " + dependency));
        }
      }

      for (Map.Entry<String, Schema> entry : schemaDependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }

        Schema schema = entry.getValue();
        schema.validate(document, path, errorConsumer);
      }
    } else if (object == JSONObject.NULL) {
      if (explicitTypes != null && !explicitTypes.contains("null")) {
        errorConsumer.accept(error(document, path, "Type mismatch"));
      }
    }

    if (_const != null) {
      if (!compare(_const, object)) {
        errorConsumer.accept(error(document, path, "Const mismatch"));
      }
    }

    if (_enum != null) {
      boolean matchedOne = false;
      for (Object value : _enum) {
        if (compare(object, value)) {
          matchedOne = true;
          break;
        }
      }
      if (!matchedOne) {
        errorConsumer.accept(error(document, path, "Object not in enum"));
      }
    }

    if (_if != null) {
      List<ValidationError> errors = new ArrayList<>();
      _if.validate(document, path, errors::add);
      Schema useSchema;
      if (errors.isEmpty()) {
        useSchema = _then;
      } else {
        useSchema = _else;
      }
      if (useSchema != null) {
        useSchema.validate(document, path, errorConsumer);
      }
    }

    if (allOf != null) {
      for (Schema schema : allOf) {
        schema.validate(document, path, errorConsumer);
      }
    }

    if (anyOf != null) {
      boolean onePassed = false;
      for (Schema schema : anyOf) {
        List<ValidationError> errors = new ArrayList<>();
        schema.validate(document, path, errors::add);
        if (errors.isEmpty()) {
          onePassed = true;
          break;
        }
      }
      if (!onePassed) {
        errorConsumer.accept(error(document, path, "All anyOf failed"));
      }
    }

    if (oneOf != null) {
      int numberPassed = 0;
      for (Schema schema : oneOf) {
        List<ValidationError> errors = new ArrayList<>();
        schema.validate(document, path, errors::add);
        if (errors.isEmpty()) {
          numberPassed++;
        }
      }
      if (numberPassed != 1) {
        errorConsumer.accept(error(document, path, numberPassed + " passed oneOf"));
      }
    }
  }

  private void typeCheck(
      Set<String> types, Object document, URI path, Consumer<ValidationError> errorConsumer) {
    if (explicitTypes == null) {
      return;
    }
    Set<String> typesIn = new HashSet<>(types);
    typesIn.retainAll(explicitTypes);
    if (!typesIn.isEmpty()) {
      return;
    }

    String part1;
    if (types.size() == 1) {
      part1 = "type " + types.iterator().next() + " ";
    } else {
      part1 = "one of: " + String.join(", ", types) + " ";
    }

    String part2;
    if (explicitTypes.size() == 1) {
      part2 = "not " + explicitTypes.iterator().next();
    } else {
      part2 = "not one of: " + String.join(", ", explicitTypes);
    }

    errorConsumer.accept(error(document, path, "Object is " + part1 + part2));

  }
}
