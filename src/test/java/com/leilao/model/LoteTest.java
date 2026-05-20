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

    // ── Desconto ──────────────────────────────────────────────────────────────

    @Test
    void descontoPercentual_calculadoCorretamente() {
        assertThat(loteBase().getDescontoPercentual())
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
    void descontoPercentual_avaliacaoNegativa_retornaNull() {
        Lote lote = loteBase();
        lote.setValorAvaliacao(new BigDecimal("-1"));
        assertThat(lote.getDescontoPercentual()).isNull();
    }

    // ── Builder defaults ──────────────────────────────────────────────────────

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

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toStringContemCamposRelevantes() {
        Lote lote = loteBase();
        lote.setId(42L);
        String repr = lote.toString();
        assertThat(repr).contains("teste", "IMOVEL_RESIDENCIAL", "300000");
    }

    // ── equals / hashCode — fix #15 ───────────────────────────────────────────

    @Test
    void equalsComMesmoId_retornaTrue() {
        Lote a = loteBase(); a.setId(1L);
        Lote b = loteBase(); b.setId(1L);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equalsComIdsDiferentes_retornaFalse() {
        Lote a = loteBase(); a.setId(1L);
        Lote b = loteBase(); b.setId(2L);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsComIdNull_retornaFalse() {
        // Lotes sem ID (transientes) nunca são iguais entre si
        Lote a = loteBase(); // id = null
        Lote b = loteBase(); // id = null
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCodeEstavelAposAlterarCampo() {
        // hashCode baseado em ID não deve mudar ao alterar outros campos
        Lote lote = loteBase();
        lote.setId(10L);
        int hcAntes = lote.hashCode();
        lote.setCidade("São Paulo");
        assertThat(lote.hashCode()).isEqualTo(hcAntes);
    }

    @Test
    void equalsComMesmoObjeto_retornaTrue() {
        Lote lote = loteBase();
        lote.setId(5L);
        assertThat(lote).isEqualTo(lote);
    }

    @Test
    void equalsComNull_retornaFalse() {
        Lote lote = loteBase();
        lote.setId(5L);
        assertThat(lote).isNotEqualTo(null);
    }

    @Test
    void equalsComTipoDiferente_retornaFalse() {
        Lote lote = loteBase();
        lote.setId(5L);
        assertThat(lote).isNotEqualTo("string");
    }
}
