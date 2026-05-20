package com.leilao.scraper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ScraperBaseTest {

    // ── limparValor ───────────────────────────────────────────────────────────

    @Test void limparValor_formatoBrasileiro() {
        assertThat(ScraperBase.limparValor("R$ 1.250.000,00"))
            .isEqualByComparingTo(new BigDecimal("1250000.00"));
    }

    @Test void limparValor_semSimbolo() {
        assertThat(ScraperBase.limparValor("350000,50"))
            .isEqualByComparingTo(new BigDecimal("350000.50"));
    }

    @Test void limparValor_nulo_retornaNull() {
        assertThat(ScraperBase.limparValor(null)).isNull();
    }

    @Test void limparValor_vazio_retornaNull() {
        assertThat(ScraperBase.limparValor("  ")).isNull();
    }

    @Test void limparValor_textoInvalido_retornaNull() {
        assertThat(ScraperBase.limparValor("valor não informado")).isNull();
    }

    // ── limparArea ────────────────────────────────────────────────────────────

    @Test void limparArea_formatoBrasileiro() {
        assertThat(ScraperBase.limparArea("120,50 m²"))
            .isEqualByComparingTo(new BigDecimal("120.50"));
    }

    @Test void limparArea_m2Minusculo() {
        assertThat(ScraperBase.limparArea("65m2"))
            .isEqualByComparingTo(new BigDecimal("65"));
    }

    @Test void limparArea_textoInvalido_retornaNull() {
        assertThat(ScraperBase.limparArea("área não informada")).isNull();
    }

    // ── parsearCidadeEstado ───────────────────────────────────────────────────

    @Test void parsearCidadeEstado_formatoSlash() {
        String[] resultado = ScraperBase.parsearCidadeEstado(
            "Av. Paulista, 1000 - Bela Vista, São Paulo/SP");
        assertThat(resultado[0]).isEqualTo("São Paulo");
        assertThat(resultado[1]).isEqualTo("SP");
    }

    @Test void parsearCidadeEstado_semSlash_retornaNull() {
        String[] resultado = ScraperBase.parsearCidadeEstado("Rua sem UF");
        assertThat(resultado[0]).isNull();
        assertThat(resultado[1]).isNull();
    }

    @Test void parsearCidadeEstado_null_retornaNull() {
        String[] resultado = ScraperBase.parsearCidadeEstado(null);
        assertThat(resultado[0]).isNull();
        assertThat(resultado[1]).isNull();
    }

    // ── mapearTipo ────────────────────────────────────────────────────────────

    @Test void mapearTipo_apartamento() {
        assertThat(ScraperBase.mapearTipo("Apartamento Residencial"))
            .isEqualTo(com.leilao.model.TipoLote.IMOVEL_RESIDENCIAL);
    }

    @Test void mapearTipo_galpao() {
        assertThat(ScraperBase.mapearTipo("Galpão Industrial"))
            .isEqualTo(com.leilao.model.TipoLote.IMOVEL_COMERCIAL);
    }

    @Test void mapearTipo_sitio() {
        assertThat(ScraperBase.mapearTipo("Sítio Rural"))
            .isEqualTo(com.leilao.model.TipoLote.IMOVEL_RURAL);
    }

    @Test void mapearTipo_veiculo() {
        assertThat(ScraperBase.mapearTipo("Veículo / Automóvel"))
            .isEqualTo(com.leilao.model.TipoLote.VEICULO);
    }

    @Test void mapearTipo_desconhecido_retornaOutros() {
        assertThat(ScraperBase.mapearTipo("Categoria Estranha"))
            .isEqualTo(com.leilao.model.TipoLote.OUTROS);
    }

    @Test void mapearTipo_null_retornaOutros() {
        assertThat(ScraperBase.mapearTipo(null))
            .isEqualTo(com.leilao.model.TipoLote.OUTROS);
    }
}
