package br.com.tauan.ms2_transacoes.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";
    private static final String TAG = "Transações";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MS2 - Transações")
                        .version("1.0.0")
                        .description("API de transações financeiras. Todas as rotas exigem autenticação básica "
                                + "(e-mail e senha de um usuário cadastrado no MS1): use o botão Authorize. "
                                + "Valores em reais; a conversão para USD e EUR usa a cotação guardada na criação."))
                .components(new Components().addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("E-mail e senha do usuário cadastrado no MS1")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }

    /**
     * Descrições, tags e exemplos em um lugar só (os controllers não precisam de anotações).
     * Rotas que não existem no projeto são ignoradas.
     */
    @Bean
    public OpenApiCustomizer docsCustomizer() {
        return openApi -> {
            describe(openApi, "/transactions", HttpMethod.POST, "Cria uma transação",
                    "Tipos: DEPOSIT, WITHDRAWAL, TRANSFER e PURCHASE. Saídas consultam o saldo e os limites na API Mock; "
                            + "uma saída que não passa nas regras é gravada como REJECTED com o motivo. "
                            + "O dono é sempre o usuário autenticado.");
            describe(openApi, "/transactions", HttpMethod.GET, "Lista as transações do usuário autenticado",
                    "Filtros opcionais: from, to (AAAA-MM-DD) e type. Mais recentes primeiro. "
                            + "Pedir as transações de outro usuário (userId) responde 403.");
            describe(openApi, "/transactions/{id}", HttpMethod.GET, "Consulta uma transação",
                    "Devolve também as cotações em USD e EUR guardadas na criação (ou conversionAvailable=false).");
            describe(openApi, "/balance", HttpMethod.GET, "Saldo atual",
                    "balance é o saldo da API Mock naquele momento; availableBalance considera os depósitos "
                            + "e as despesas aprovadas do dia.");
            describe(openApi, "/expenses/summary", HttpMethod.GET, "Resumo de despesas",
                    "Total geral e por categoria, por dia e por mês. Só despesas aprovadas. "
                            + "Sem from e to, vale o mês atual.");
            describe(openApi, "/reports/transactions", HttpMethod.GET, "Relatório de transações em CSV",
                    "Arquivo CSV para o Excel (UTF-8 com BOM, separador ponto e vírgula). "
                            + "Sem from e to, vale o mês atual.");
            addTransactionExamples(openApi);
            addCommonResponses(openApi);
        };
    }

    private void describe(OpenAPI openApi, String path, HttpMethod method, String summary, String description) {
        Operation operation = operation(openApi, path, method);
        if (operation == null) {
            return;
        }
        operation.setTags(List.of(TAG));
        operation.setSummary(summary);
        operation.setDescription(description);
    }

    private Operation operation(OpenAPI openApi, String path, HttpMethod method) {
        if (openApi.getPaths() == null) {
            return null;
        }
        PathItem item = openApi.getPaths().get(path);
        return item == null ? null : item.readOperationsMap().get(method);
    }

    private void addTransactionExamples(OpenAPI openApi) {
        Operation post = operation(openApi, "/transactions", HttpMethod.POST);
        if (post == null || post.getRequestBody() == null || post.getRequestBody().getContent() == null) {
            return;
        }
        MediaType json = post.getRequestBody().getContent().get("application/json");
        if (json == null) {
            return;
        }
        json.addExamples("Depósito", example("Depósito de salário",
                body("type", "DEPOSIT", "amount", 1500.00, "description", "Salário")));
        json.addExamples("Compra", example("Compra no mercado",
                body("type", "PURCHASE", "amount", 89.90, "description", "Mercado", "category", "FOOD")));
        json.addExamples("Retirada", example("Saque",
                body("type", "WITHDRAWAL", "amount", 200.00, "description", "Saque")));
        json.addExamples("Transferência", example("Transferência para outro usuário",
                body("type", "TRANSFER", "amount", 50.00, "description", "Pix para um amigo", "destinationUserId", 2)));
    }

    private void addCommonResponses(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        for (PathItem item : openApi.getPaths().values()) {
            for (Operation operation : item.readOperationsMap().values()) {
                if (operation.getResponses() == null) {
                    operation.setResponses(new ApiResponses());
                }
                operation.getResponses().addApiResponse("401",
                        new ApiResponse().description("Sem credenciais ou credenciais inválidas"));
                operation.getResponses().addApiResponse("503",
                        new ApiResponse().description("Serviço de usuários ou API Mock indisponível"));
            }
        }
    }

    private static Example example(String summary, Map<String, Object> value) {
        Example example = new Example();
        example.setSummary(summary);
        example.setValue(value);
        return example;
    }

    private static Map<String, Object> body(Object... keysAndValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            body.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return body;
    }
}