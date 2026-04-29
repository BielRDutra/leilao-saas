package com.leilao.dto;

import com.leilao.model.Lote;
import com.leilao.model.TipoLote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Versão compacta de um lote para exibição no ranking.
 * Inclui só os campos relevantes para tomada de decisão rápida.
 */
public record RankingItemDTO(
    Long          id,
    String        fonte,
    String        urlOriginal,
    TipoLote      tipo,
    String        cidade,
    String        estado,
    BigDecimal    valorLanceInicial,
    BigDecimal    valorAvaliacao,
    BigDecimal    descontoPercentual,
    boolean       aceitaFinanciamento,
    boolean       aceitaFgts,
    BigDecimal    scoreOportunidade,
    String        classificacao,
    LocalDateTime dataLeilao
) {
    public static RankingItemDTO from(Lote lote) {
        double score = lote.getScoreOportunidade() != null
            ? lote.getScoreOportunidade().doubleValue() : 0.0;

        String classif;
        if (score >= 80)      classif = "Excelente";
        else if (score >= 65) classif = "Muito bom";
        else if (score >= 50) classif = "Bom";
        else if (score >= 35) classif = "Regular";
        else                  classif = "Baixo";

        return new RankingItemDTO(
            lote.getId(),
            lote.getFonte(),
            lote.getUrlOriginal(),
            lote.getTipo(),
            lote.getCidade(),
            lote.getEstado(),
            lote.getValorLanceInicial(),
            lote.getValorAvaliacao(),
            lote.getDescontoPercentual(),
            lote.isAceitaFinanciamento(),
            lote.isAceitaFgts(),
            lote.getScoreOportunidade(),
            classif,
            lote.getDataLeilao()
        );
    }
}
