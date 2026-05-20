package com.leilao.dto;

import com.leilao.model.Lote;
import com.leilao.model.TipoLote;
import com.leilao.util.Classificacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO compacto para exibição no ranking.
 * Fix #14: classificação delegada para Classificacao.de().
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
            Classificacao.de(lote.getScoreOportunidade()), // Fix #14
            lote.getDataLeilao()
        );
    }
}
