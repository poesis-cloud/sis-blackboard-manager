package cloud.poesis.sie.bbman.service;

import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * JSON Schema retrieval, caching and validation for slot {@code schemaUri}s.
 *
 * <p>
 * Resolution and caching are done via the networknt {@link JsonSchemaFactory};
 * resolution
 * failures surface as {@code RuntimeException} so callers can map them to the
 * appropriate rule
 * violation.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Service
public class JsonSchemaValidationService {

  private final JsonSchemaFactory schemaFactory;
  private final Map<String, JsonSchema> cache = new ConcurrentHashMap<>();

  public JsonSchemaValidationService() {
    this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  }

  /**
   * Loads (and caches) the schema referenced by {@code schemaUri}, throwing a
   * runtime exception on
   * resolution failure.
   *
   * @param schemaUri the JSON Schema URI
   */
  public void assertResolvable(URI schemaUri) {
    load(schemaUri);
  }

  /**
   * Validates {@code instance} against the schema at {@code schemaUri}. On any
   * validation message,
   * raises a {@link RuleViolationException} ({@code 422}).
   *
   * @param schemaUri the JSON Schema URI
   * @param instance  the JSON payload
   */
  public void validate(URI schemaUri, JsonNode instance) {
    JsonSchema schema = load(schemaUri);
    Set<ValidationMessage> messages = schema.validate(instance);
    if (!messages.isEmpty()) {
      String detail = messages.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; "));
      Map<String, Object> site = new LinkedHashMap<>();
      site.put("schemaUri", schemaUri);
      site.put(
          "messages",
          messages.stream().map(ValidationMessage::getMessage).collect(Collectors.toList()));
      throw new RuleViolationException(
          BlackboardConsistencyRuleType.CONTRIBUTION_POST_SCHEMA_VIOLATION, detail, site);
    }
  }

  private JsonSchema load(URI schemaUri) {
    return cache.computeIfAbsent(schemaUri.toString(), key -> {
      SchemaValidatorsConfig config = new SchemaValidatorsConfig();
      try {
        return schemaFactory.getSchema(SchemaLocation.of(key), config);
      } catch (Exception e) {
        throw new IllegalStateException("Cannot load schema at " + schemaUri + ": " + e.getMessage(), e);
      }
    });
  }
}
