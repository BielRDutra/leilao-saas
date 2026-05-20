package com.leilao.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o utilitário centralizado de classificação — fix #14.
 */
class ClassificacaoTest {

    @ParameterizedTest(name = "score {0} → {1}")
    @CsvSource({
        "80.0,  Excelente",
        "85.0,  Excelente",
        "100.0, Excelente",
        "65.0,  Muito bom",
        "79.9,  Muito bom",
        "50.0,  Bom",
        "64.9,  Bom",
        "35.0,  Regular",
        "49.9,  Regular",
        "0.0,   Baixo",
        "34.9,  Baixo",
    })
    void classificacaoCorreta(double score, String esperado) {
        assertThat(Classificacao.de(score)).isEqualTo(esperado);
    }

    @Test
    void scoreNull_retornaComLabel() {
        assertThat(Classificacao.de((Double) null)).isEqualTo("Sem score");
    }

    @Test
    void bigDecimal_delegaParaDouble() {
        assertThat(Classificacao.de(new BigDecimal("72.5"))).isEqualTo("Muito bom");
    }

    @Test
    void bigDecimalNull_retornaComLabel() {
        assertThat(Classificacao.de((BigDecimal) null)).isEqualTo("Sem score");
    }

    @Test
    void limiteExato80_retornaExcelente() {
        // Verifica fronteiras exatas (boundary testing)
        assertThat(Classificacao.de(80.0)).isEqualTo("Excelente");
        assertThat(Classificacao.de(79.9)).isEqualTo("Muito bom");
    }

    @Test
    void limiteExato65_retornaMuitoBom() {
        assertThat(Classificacao.de(65.0)).isEqualTo("Muito bom");
        assertThat(Classificacao.de(64.9)).isEqualTo("Bom");
    }

    @Test
    void limiteExato50_retornaBom() {
        assertThat(Classificacao.de(50.0)).isEqualTo("Bom");
        assertThat(Classificacao.de(49.9)).isEqualTo("Regular");
    }

    @Test
    void limiteExato35_retornaRegular() {
        assertThat(Classificacao.de(35.0)).isEqualTo("Regular");
        assertThat(Classificacao.de(34.9)).isEqualTo("Baixo");
    }
}
