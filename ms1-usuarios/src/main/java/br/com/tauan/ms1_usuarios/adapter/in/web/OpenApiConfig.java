package br.com.tauan.ms1_usuarios.adapter.in.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ms1OpenApi() {
        return new OpenAPI().info(new Info()
                .title("MS1 User Service")
                .version("1.0.0")
                .description("Cadastro e manutenção de usuários, com importação em massa por CSV."));
    }
}