package com.leilao.dto;

import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;
import com.leilao.util.Classificacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta padrão para um lote de leilão.
 * Fix #14: classificação delegada para Classificacao.de() — elimina duplicação.
 */
public record LoteDTO(
    Long          id,
    String        fonte,
    String        urlOriginal,
    TipoLote      tipo,
    OrigemLeilao  origem,
    StatusLote    status,
    BigDecimal    valorAvaliacao,
    BigDecimal    valorLanceInicial,
    BigDecimal    descontoPercentual,
    boolean       aceitaFinanciamento,
    boolean       aceitaFgts,
    String        bancoFinanciador,
    String        logradouro,
    String        bairro,
    String        cidade,
    String        estado,
    String        cep,
    BigDecimal    latitude,
    BigDecimal    longitude,
    String        descricao,
    BigDecimal    areaM2,
    Boolean       ocupado,
    LocalDateTime dataLeilao,
    LocalDateTime dataPrimeiroLeilao,
    LocalDateTime dataSegundoLeilao,
    BigDecimal    scoreOportunidade,
    BigDecimal    scoreDesconto,
    BigDecimal    scoreFinanciamento,
    BigDecimal    scoreLocalizacao,
    BigDecimal    scoreRisco,
    String        classificacao,
    LocalDateTime coletadoEm
) {
    public static LoteDTO from(Lote lote) {
        return new LoteDTO(
            lote.getId(),
            lote.getFonte(),
            lote.getUrlOriginal(),
            lote.getTipo(),
            lote.getOrigem(),
            lote.getStatus(),
            lote.getValorAvaliacao(),
            lote.getValorLanceInicial(),
            lote.getDescontoPercentual(),
            lote.isAceitaFinanciamento(),
            lote.isAceitaFgts(),
            lote.getBancoFinanciador(),
            lote.getLogradouro(),
            lote.getBairro(),
            lote.getCidade(),
            lote.getEstado(),
            lote.getCep(),
            lote.getLatitude(),
            lote.getLongitude(),
            lote.getDescricao(),
            lote.getAreaM2(),
            lote.getOcupado(),
            lote.getDataLeilao(),
            lote.getDataPrimeiroLeilao(),
            lote.getDataSegundoLeilao(),
            lote.getScoreOportunidade(),
            lote.getScoreDesconto(),
            lote.getScoreFinanciamento(),
            lote.getScoreLocalizacao(),
            lote.getScoreRisco(),
            Classificacao.de(lote.getScoreOportunidade()), // Fix #14
            lote.getColetadoEm()
        );
    }
}
