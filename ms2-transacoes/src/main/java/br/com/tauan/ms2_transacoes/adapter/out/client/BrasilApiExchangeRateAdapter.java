package br.com.tauan.ms2_transacoes.adapter.out.client;

import br.com.tauan.ms2_transacoes.application.port.out.ExchangeRatePort;
import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class BrasilApiExchangeRateAdapter implements ExchangeRatePort {

    private static final Logger log = LoggerFactory.getLogger(BrasilApiExchangeRateAdapter.class);
    private static final String CLOSING_BULLETIN = "FECHAMENTO";

    private final RestClient restClient;
    private final Clock clock;

    public BrasilApiExchangeRateAdapter(@Qualifier("brasilApiRestClient") RestClient restClient, Clock clock) {
        this.restClient = restClient;
        this.clock = clock;
    }

    @Override
    public Optional<ExchangeRate> findLatest(String currency) {
        // A fonte não expõe o dia corrente: pede ontem. Em dia sem pregão (fim de semana, feriado),
        // a própria BrasilAPI devolve o último dia útil anterior, e a data da resposta é a que vale.
        LocalDate requested = LocalDate.now(clock).minusDays(1);
        try {
            BrasilApiQuoteResponse response = restClient.get()
                    .uri("/cambio/v1/cotacao/{currency}/{date}", currency, requested)
                    .retrieve()
                    .body(BrasilApiQuoteResponse.class);
            return toExchangeRate(currency, requested, response);
        } catch (RuntimeException e) {
            log.warn("Exchange rate lookup failed for {}: {}", currency, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ExchangeRate> toExchangeRate(String currency, LocalDate requested,
                                                  BrasilApiQuoteResponse response) {
        if (response == null || response.cotacoes() == null || response.cotacoes().isEmpty()) {
            return Optional.empty();
        }
        List<BrasilApiQuoteResponse.Quote> quotes = response.cotacoes();
        // O boletim de fechamento vem como "FECHAMENTO PTAX". Sem ele, vale o último boletim do dia.
        BrasilApiQuoteResponse.Quote quote = quotes.stream()
                .filter(q -> q.tipoBoletim() != null
                        && q.tipoBoletim().toUpperCase().startsWith(CLOSING_BULLETIN))
                .findFirst()
                .orElse(quotes.get(quotes.size() - 1));
        if (quote.cotacaoVenda() == null) {
            return Optional.empty();
        }
        LocalDate rateDate = response.data() == null ? requested : LocalDate.parse(response.data());
        return Optional.of(new ExchangeRate(currency, quote.cotacaoVenda(), rateDate));
    }
}