package it.brunasti.dbdadi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dbdadiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("dbdadi - DB Data Dictionary API")
                        .description("REST API for managing data dictionaries of multiple database models")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("brunasti")
                                .url("https://github.com/brunasti"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
