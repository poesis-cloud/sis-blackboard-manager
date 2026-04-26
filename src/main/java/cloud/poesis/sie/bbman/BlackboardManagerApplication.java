package cloud.poesis.sie.bbman;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the SIE Definition Blackboard Manager service.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@OpenAPIDefinition(
    info =
        @Info(
            title = "SIE Definition Blackboard Manager API",
            version = "v1",
            description =
                "Generic, domain-agnostic REST blackboard service for collaborative "
                    + "definition sourcing. Owns Blackboard data and seal lifecycle. "
                    + "Does not interpret domain ontology and does not reduce contributions; "
                    + "client sourcers declare panels and slot schemas as data."))
@SpringBootApplication
public class BlackboardManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(BlackboardManagerApplication.class, args);
  }
}
