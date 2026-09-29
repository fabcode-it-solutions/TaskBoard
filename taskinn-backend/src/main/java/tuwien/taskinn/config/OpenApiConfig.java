package tuwien.taskinn.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // TODO: Add contact information
        return new OpenAPI().info(new Info().title("TaskInn Auth API")
                .version("1.0")
                .description("Authentication and Authorization API for TaskInn")
                .contact(new Contact().name("TaskInn Team").email("unknown@taskinn.com")));
    }
}
