package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrasilApiExchangeRateAdapterTest {

    // 2026-11-10 12:00 em Brasília: a cotação pedida é a de ontem (2026-11-09)
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));
    private static final String URL = "http://brasilapi.test/api/cambio/v1/cotacao/USD/2026-11-09";

    // Resposta real da BrasilAPI (nomes de campo com underline e boletim "FECHAMENTO PTAX")
    private static final String REAL_RESPONSE = """
            {"cotacoes":[
              {"paridade_compra":1,"paridade_venda":1,"cotacao_compra":5.5059,"cotacao_venda":5.5065,
               "data_hora_cotacao":"2026-11-09 10:09:14.44952","tipo_boletim":"ABERTURA"},
              {"paridade_compra":1,"paridade_venda":1,"cotacao_compra":5.5121,"cotacao_venda":5.5127,
               "data_hora_cotacao":"2026-11-09 11:06:10.110274","tipo_boletim":"INTERMEDIÁRIO"},
              {"paridade_compra":1,"paridade_venda":1,"cotacao_compra":5.514,"cotacao_venda":5.5146,
               "data_hora_cotacao":"2026-11-09 13:11:19.041465","tipo_boletim":"FECHAMENTO PTAX"}],
             "moeda":"USD","data":"2026-11-09"}
            """;

    private MockRestServiceServer server;
    private BrasilApiExchangeRateAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://brasilapi.test/api");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new BrasilApiExchangeRateAdapter(builder.build(), CLOCK);
    }

    @Test
    void findLatest_withTheRealResponse_usesTheSaleQuoteOfTheClosingPtaxBulletin() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(REAL_RESPONSE, MediaType.APPLICATION_JSON));

        Optional<ExchangeRate> rate = adapter.findLatest("USD");

        assertThat(rate).isPresent();
        assertThat(rate.get().currency()).isEqualTo("USD");
        assertThat(rate.get().rate()).isEqualByComparingTo("5.5146");
        assertThat(rate.get().rateDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        server.verify();
    }

    @Test
    void findLatest_onANonBusinessDay_keepsTheDateOfTheResponse() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"moeda":"USD","data":"2026-11-06","cotacoes":[
                          {"cotacao_compra":5.40,"cotacao_venda":5.41,"tipo_boletim":"FECHAMENTO PTAX"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findLatest("USD").orElseThrow().rateDate()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void findLatest_withoutAClosingBulletin_usesTheLastOneOfTheDay() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"moeda":"USD","data":"2026-11-09","cotacoes":[
                          {"cotacao_compra":5.30,"cotacao_venda":5.31,"tipo_boletim":"ABERTURA"},
                          {"cotacao_compra":5.35,"cotacao_venda":5.36,"tipo_boletim":"INTERMEDIARIO"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findLatest("USD").orElseThrow().rate()).isEqualByComparingTo("5.36");
    }

    @Test
    void findLatest_whenThereIsNoQuote_returnsEmpty() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"moeda":"USD","data":"2026-11-09","cotacoes":[]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findLatest("USD")).isEmpty();
    }

    @Test
    void findLatest_whenTheSaleQuoteIsMissing_returnsEmpty() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"moeda":"USD","data":"2026-11-09","cotacoes":[{"tipo_boletim":"FECHAMENTO PTAX"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(adapter.findLatest("USD")).isEmpty();
    }

    @Test
    void findLatest_whenTheServiceFails_returnsEmptyWithoutThrowing() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(adapter.findLatest("USD")).isEmpty();
    }

    @Test
    void findLatest_whenTheServiceAnswersNotFound_returnsEmptyWithoutThrowing() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThat(adapter.findLatest("USD")).isEmpty();
    }

    @Test
    void findLatest_withAnUnreadableBody_returnsEmptyWithoutThrowing() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("isto nao e json", MediaType.APPLICATION_JSON));

        assertThat(adapter.findLatest("USD")).isEmpty();
    }
}