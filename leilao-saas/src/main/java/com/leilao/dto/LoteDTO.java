package com.leilao.dto;

import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta padrão para um lote de leilão.
 * Nunca exponha a entidade JPA diretamente na API — use sempre este DTO.
 */
public record LoteDTO(
    Long          id,
    String        fonte,
    String        urlOriginal,
    TipoLote      tipo,
    OrigemLeilao  origem,
    StatusLote    status,

    // Valores
    BigDecimal    valorAvaliacao,
    BigDecimal    valorLanceInicial,
    BigDecimal    descontoPercentual,

    // Financiamento
    boolean       aceitaFinanciamento,
    boolean       aceitaFgts,
    String        bancoFinanciador,

    // Localização
    String        logradouro,
    String        bairro,
    String        cidade,
    String        estado,
    String        cep,
    BigDecimal    latitude,
    BigDecimal    longitude,

    // Detalhes
    String        descricao,
    BigDecimal    areaM2,
    Boolean       ocupado,

    // Datas
    LocalDateTime dataLeilao,
    LocalDateTime dataPrimeiroLeilao,
    LocalDateTime dataSegundoLeilao,

    // Score
    BigDecimal    scoreOportunidade,
    BigDecimal    scoreDesconto,
    BigDecimal    scoreFinanciamento,
    BigDecimal    scoreLocalizacao,
    BigDecimal    scoreRisco,
    String        classificacao,

    // Controle
    LocalDateTime coletadoEm
) {
    /** Converte uma entidade Lote para o DTO de resposta. */
    public static LoteDTO from(Lote lote) {
        String classif = classificar(
            lote.getScoreOportunidade() != null
                ? lote.getScoreOportunidade().doubleValue() : null
        );

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
            classif,
            lote.getColetadoEm()
        );
    }

    private static String classificar(Double score) {
        if (score == null)   return "Sem score";
        if (score >= 80)     return "Excelente";
        if (score >= 65)     return "Muito bom";
        if (score >= 50)     return "Bom";
        if (score >= 35)     return "Regular";
        return "Baixo";
    }
}
