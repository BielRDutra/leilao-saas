package com.leilao.score;

import com.leilao.config.ScoreConfig;
import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * As 4 dimensões do motor de score.
 * Equivalente ao score/dimensoes.py do Python.
 *
 * Cada dimensão recebe um Lote e retorna um ResultadoDimensao (0–100).
 */
public class DimensoesScore {

    // Impede instanciação — só classes internas estáticas
    private DimensoesScore() {}

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 1 — Desconto (peso 40%)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ScoreDesconto {
        private final ScoreConfig config;

        public ScoreDesconto(ScoreConfig config) { this.config = config; }

        /**
         * Curva logarítmica: descontos maiores pontuam desproporcionalmente mais.
         * < 10%  → 0 | 20% → ~40 | 30% → ~62 | ≥ 50% → 100
         */
        public ResultadoDimensao calcular(Lote lote) {
            Map<String, Object> detalhes = new HashMap<>();

            if (lote.getValorAvaliacao() == null
                    || lote.getValorAvaliacao().compareTo(BigDecimal.ZERO) <= 0) {
                detalhes.put("motivo", "sem valor de avaliação");
                return new ResultadoDimensao("desconto", 0.0, detalhes);
            }

            double avaliacao = lote.getValorAvaliacao().doubleValue();
            double lance     = lote.getValorLanceInicial().doubleValue();
            double descPct   = (1.0 - lance / avaliacao) * 100.0;

            detalhes.put("desconto_pct",     Math.round(descPct * 100.0) / 100.0);
            detalhes.put("valor_avaliacao",  avaliacao);
            detalhes.put("valor_lance",      lance);

            double dMin = config.getDescontoMinPct();
            double dMax = config.getDescontoMaxPct();

            double valor;
            if (descPct <= 0) {
                valor = 0.0;
                detalhes.put("motivo", "lance acima ou igual à avaliação");
            } else if (descPct < dMin) {
                valor = 0.0;
                detalhes.put("motivo", "desconto abaixo do mínimo (" + dMin + "%)");
            } else if (descPct >= dMax) {
                valor = 100.0;
                detalhes.put("motivo", "desconto máximo atingido (≥ " + dMax + "%)");
            } else {
                // Normaliza para [0,1] e aplica curva log natural
                double norm = (descPct - dMin) / (dMax - dMin);
                valor = Math.log1p(norm * (Math.E - 1)) * 100.0;
                valor = Math.round(valor * 100.0) / 100.0;
                detalhes.put("motivo", "curva logarítmica aplicada");
            }

            return new ResultadoDimensao("desconto", valor, detalhes);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 2 — Financiamento (peso 25%)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ScoreFinanciamento {
        private final ScoreConfig config;

        public ScoreFinanciamento(ScoreConfig config) { this.config = config; }

        public ResultadoDimensao calcular(Lote lote) {
            Map<String, Object> detalhes = new HashMap<>();
            List<String> bonuses = new ArrayList<>();
            double pontos = 0.0;

            if (lote.isAceitaFinanciamento()) {
                pontos += config.getBonusFinanciamento();
                bonuses.add("+%.0f financiamento".formatted(config.getBonusFinanciamento()));
            }
            if (lote.isAceitaFgts()) {
                pontos += config.getBonusFgts();
                bonuses.add("+%.0f FGTS".formatted(config.getBonusFgts()));
            }

            String banco = lote.getBancoFinanciador() != null
                ? lote.getBancoFinanciador().toLowerCase() : "";
            if (banco.contains("caixa") || banco.contains("cef")) {
                pontos += config.getBonusCaixa();
                bonuses.add("+%.0f banco Caixa".formatted(config.getBonusCaixa()));
            }

            detalhes.put("bonuses", bonuses);
            if (bonuses.isEmpty()) {
                detalhes.put("motivo", "sem opções de financiamento (apenas à vista)");
            }

            return new ResultadoDimensao("financiamento", Math.min(pontos, 100.0), detalhes);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 3 — Localização (peso 20%)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ScoreLocalizacao {
        private final ScoreConfig config;

        public ScoreLocalizacao(ScoreConfig config) { this.config = config; }

        public ResultadoDimensao calcular(Lote lote) {
            Map<String, Object> detalhes = new HashMap<>();
            List<String> bonuses = new ArrayList<>();

            if (lote.getEstado() == null && lote.getCidade() == null) {
                detalhes.put("motivo", "sem informação de localização");
                return new ResultadoDimensao("localizacao", 0.0, detalhes);
            }

            double pontos = 40.0;
            bonuses.add("+40 localização informada");

            // Estado premium
            if (lote.getEstado() != null
                    && config.getEstadosPremium().contains(lote.getEstado().toUpperCase())) {
                pontos += config.getBonusEstadoPremium();
                bonuses.add("+%.0f estado premium (%s)".formatted(
                    config.getBonusEstadoPremium(), lote.getEstado()));
            }

            // Cidade premium
            String cidade = lote.getCidade() != null ? lote.getCidade() : "";
            boolean cidadePremium = config.getCidadesPremium().stream()
                .anyMatch(c -> cidade.toLowerCase().contains(c.toLowerCase()));
            if (cidadePremium) {
                pontos += config.getBonusCidadePremium();
                bonuses.add("+%.0f cidade premium (%s)".formatted(
                    config.getBonusCidadePremium(), cidade));
            }

            // Coordenadas (geocodificado)
            if (lote.getLatitude() != null && lote.getLongitude() != null) {
                pontos += 10.0;
                bonuses.add("+10 geocodificado");
            }

            // Bairro informado
            if (lote.getBairro() != null && !lote.getBairro().isBlank()) {
                pontos += 10.0;
                bonuses.add("+10 bairro informado");
            }

            detalhes.put("bonuses", bonuses);
            return new ResultadoDimensao("localizacao", Math.min(pontos, 100.0), detalhes);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 4 — Risco (peso 15%)
    // ══════════════════════════════════════════════════════════════════════════

    public static class ScoreRisco {
        private final ScoreConfig config;

        public ScoreRisco(ScoreConfig config) { this.config = config; }

        /**
         * Começa em 100 e desconta penalidades.
         * Leilão judicial ganha bônus — tende a ter descontos estruturais maiores.
         */
        public ResultadoDimensao calcular(Lote lote) {
            Map<String, Object> detalhes = new HashMap<>();
            List<String> penalidades = new ArrayList<>();
            List<String> bonuses     = new ArrayList<>();
            double pontos = 100.0;

            // Penalidade: imóvel ocupado
            if (Boolean.TRUE.equals(lote.getOcupado())) {
                pontos -= config.getPenaltOcupado();
                penalidades.add("-%.0f imóvel ocupado (risco desocupação)"
                    .formatted(config.getPenaltOcupado()));
            }

            // Penalidade: sem valor de avaliação
            if (lote.getValorAvaliacao() == null) {
                pontos -= config.getPenaltSemAvaliacao();
                penalidades.add("-%.0f sem valor de avaliação"
                    .formatted(config.getPenaltSemAvaliacao()));
            }

            // Penalidade: 2ª praça
            if (lote.getDataSegundoLeilao() != null && lote.getDataPrimeiroLeilao() != null) {
                pontos -= config.getPenaltSegundaPraca();
                penalidades.add("-%.0f 2ª praça (leilão anterior fracassou)"
                    .formatted(config.getPenaltSegundaPraca()));
            }

            // Bônus: leilão judicial
            if (OrigemLeilao.JUDICIAL.equals(lote.getOrigem())) {
                pontos += config.getBonusJudicial();
                bonuses.add("+%.0f leilão judicial (desconto estrutural)"
                    .formatted(config.getBonusJudicial()));
            }

            detalhes.put("penalidades", penalidades);
            detalhes.put("bonuses",     bonuses);

            double valor = Math.max(0.0, Math.min(pontos, 100.0));
            return new ResultadoDimensao("risco", valor, detalhes);
        }
    }
}
