package br.com.tauan.ms2_transacoes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    private OpenAPI openApiWithTransactions() {
        Operation post = new Operation()
                .requestBody(new RequestBody().content(new Content().addMediaType("application/json", new MediaType())))
                .responses(new ApiResponses());
        Operation get = new Operation().responses(new ApiResponses());
        return new OpenAPI().paths(new Paths().addPathItem("/transactions", new PathItem().post(post).get(get)));
    }

    @Test
    void openApi_declaresTheBasicAuthScheme_forAllRoutes() {
        OpenAPI openApi = config.openAPI();

        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("basicAuth");
        assertThat(openApi.getComponents().getSecuritySchemes().get("basicAuth").getScheme()).isEqualTo("basic");
        assertThat(openApi.getSecurity()).isNotEmpty();
    }

    @Test
    void customizer_describesTheRoutes_andAddsTheExamplesToTheCreation() {
        OpenAPI openApi = openApiWithTransactions();

        config.docsCustomizer().customise(openApi);

        PathItem item = openApi.getPaths().get("/transactions");
        assertThat(item.getPost().getSummary()).isEqualTo("Cria uma transação");
        assertThat(item.getPost().getTags()).containsExactly("Transações");
        assertThat(item.getGet().getSummary()).isEqualTo("Lista as transações do usuário autenticado");
        assertThat(item.getPost().getRequestBody().getContent().get("application/json").getExamples())
                .containsKeys("Depósito", "Compra", "Retirada", "Transferência");
    }

    @Test
    void customizer_addsTheCommonErrorResponses_toEveryOperation() {
        OpenAPI openApi = openApiWithTransactions();

        config.docsCustomizer().customise(openApi);

        PathItem item = openApi.getPaths().get("/transactions");
        assertThat(item.getPost().getResponses()).containsKeys("401", "503");
        assertThat(item.getGet().getResponses()).containsKeys("401", "503");
    }

    @Test
    void customizer_ignoresRoutesThatDoNotExist() {
        OpenAPI withoutPaths = new OpenAPI();

        assertThatCode(() -> config.docsCustomizer().customise(withoutPaths)).doesNotThrowAnyException();
    }
}