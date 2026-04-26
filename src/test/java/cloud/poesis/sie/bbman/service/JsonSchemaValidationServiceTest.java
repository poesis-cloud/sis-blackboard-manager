package cloud.poesis.sie.bbman.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.sie.bbman.exception.RuleViolationException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import org.junit.jupiter.api.Test;

class JsonSchemaValidationServiceTest {

  private final JsonSchemaValidationService svc = new JsonSchemaValidationService();

  @Test
  void validatePassesForValidObject() {
    URI uri = URI.create("classpath:schemas/passthrough.json");
    ObjectNode n = JsonNodeFactory.instance.objectNode().put("a", 1);
    svc.assertResolvable(uri);
    svc.validate(uri, n); // no throw
    svc.validate(uri, n); // hits cache
  }

  @Test
  void validateFailsForArray() {
    URI uri = URI.create("classpath:schemas/passthrough.json");
    var arr = JsonNodeFactory.instance.arrayNode();
    arr.add(1);
    assertThatThrownBy(() -> svc.validate(uri, arr))
        .isInstanceOf(RuleViolationException.class);
  }

  @Test
  void unresolvableSchemaThrows() {
    URI uri = URI.create("classpath:schemas/does-not-exist.json");
    // Resolution may be lazy; call validate to force fetch.
    assertThatThrownBy(() -> svc.validate(uri, JsonNodeFactory.instance.objectNode()))
        .isInstanceOf(Exception.class);
  }
}
