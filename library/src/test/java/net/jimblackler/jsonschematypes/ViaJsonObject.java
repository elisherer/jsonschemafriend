package net.jimblackler.jsonschematypes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationError;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class ViaJsonObject {
  @Test
  public void viaJsonObject() throws SchemaException {
    JSONObject mySchema = new JSONObject();
    mySchema.put("$schema", "http://json-schema.org/draft-07/schema#");
    JSONObject properties = new JSONObject();
    JSONObject myValue = new JSONObject();
    myValue.put("type", "integer");
    properties.put("myValue", myValue);
    mySchema.put("properties", properties);
    Validator validator = new Validator();

    Schema schema = new SchemaStore().loadSchema(mySchema);

    {
      JSONObject myObject = new JSONObject();
      myObject.put("myValue", "x");
      List<ValidationError> errors = new ArrayList<>();
      validator.validate(schema, myObject, errors::add);
      assertFalse(errors.isEmpty());
    }

    {
      JSONObject myObject = new JSONObject();
      myObject.put("myValue", 1);
      List<ValidationError> errors = new ArrayList<>();
      validator.validate(schema, myObject, errors::add);
      assertTrue(errors.isEmpty());
    }
  }
}
