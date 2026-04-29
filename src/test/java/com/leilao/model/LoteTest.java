package com.leilao.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class LoteTest {

    private Lote loteBase() {
        return Lote.builder()
            .fonte("teste")
            .idExterno("001")
            .urlOriginal("http://exemplo.com/lote/001")
            .tipo(TipoLote.IMOVEL_RESIDENCIAL)
            .status(StatusLote.DISPONIVEL)
            .valorAvaliacao(new BigDecimal("500000.00"))
            .valorLanceInicial(new BigDecimal("300000.00"))
            .build();
    }

    @Test
    void descontoPercentual_calculadoCorretamente() {
        // Avaliação 500k, lance 300k → 40% de desconto
        Lote lote = loteBase();
        assertThat(lote.getDescontoPercentual())
            .isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    void descontoPercentual_semAvaliacao_retornaNull() {
        Lote lote = loteBase();
        lote.setValorAvaliacao(null);
        assertThat(lote.getDescontoPercentual()).isNull();
    }

    @Test
    void descontoPercentual_avaliacaoZero_retornaNull() {
        Lote lote = loteBase();
        lote.setValorAvaliacao(BigDecimal.ZERO);
        assertThat(lote.getDescontoPercentual()).isNull();
    }

    @Test
    void toStringContemCamposRelevantes() {
        Lote lote = loteBase();
        lote.setId(42L);
        String repr = lote.toString();
        assertThat(repr).contains("teste", "IMOVEL_RESIDENCIAL", "300000");
    }

    @Test
    void builderDefaults_statusDisponivel() {
        Lote lote = Lote.builder()
            .fonte("x").idExterno("1").urlOriginal("http://x.com")
            .valorLanceInicial(BigDecimal.TEN)
            .build();
        assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
        assertThat(lote.getTipo()).isEqualTo(TipoLote.OUTROS);
        assertThat(lote.isAceitaFinanciamento()).isFalse();
        assertThat(lote.isAceitaFgts()).isFalse();
    }
}
