package com.leilao.util;

/**
 * Utilitário de classificação de score — centraliza a lógica que estava
 * duplicada em LoteDTO, RankingItemDTO e ResultadoScore.
 * Fix #14: eliminação de código duplicado.
 */
public final class Classificacao {

    private Classificacao() {}

    public static String de(Double score) {
        if (score == null)  return "Sem score";
        if (score >= 80)    return "Excelente";
        if (score >= 65)    return "Muito bom";
        if (score >= 50)    return "Bom";
        if (score >= 35)    return "Regular";
        return "Baixo";
    }

    public static String de(java.math.BigDecimal score) {
        return de(score != null ? score.doubleValue() : null);
    }
}
