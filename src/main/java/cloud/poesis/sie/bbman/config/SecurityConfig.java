package cloud.poesis.sie.bbman.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for Blackboard Manager.
 *
 * <p>During the design phase, REST endpoints under {@code /api/v1/blackboards/**} are open
 * ({@code permitAll}); JWT bearer auth wires the resource server when a JWK set URI is configured.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Configuration
public class SecurityConfig {

  @Value("${bbm.security.oauth2-login-enabled:false}")
  private boolean oauth2LoginEnabled;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health/**",
                        "/actuator/info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/api/v1/blackboards/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    if (oauth2LoginEnabled) {
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    }
    return http.build();
  }
}
