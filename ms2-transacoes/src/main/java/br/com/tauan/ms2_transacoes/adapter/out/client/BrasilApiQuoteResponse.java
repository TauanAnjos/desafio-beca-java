package br.com.tauan.ms2_transacoes.adapter.out.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiQuoteResponse(String moeda, String data, List<Quote> cotacoes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
            @JsonProperty("cotacao_compra") BigDecimal cotacaoCompra,
            @JsonProperty("cotacao_venda") BigDecimal cotacaoVenda,
            @JsonProperty("tipo_boletim") String tipoBoletim
    ) {
    }
}