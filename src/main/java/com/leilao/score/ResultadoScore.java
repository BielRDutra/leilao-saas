package com.leilao.score;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Score completo de um lote, com detalhes de cada dimensão.
 * Equivalente ao @dataclass ResultadoScore do Python.
 */
public record ResultadoScore(
    Long   loteId,
    String fonte,
    String idExterno,

    double scoreDesconto,
    double scoreFinanciamento,
    double scoreLocalizacao,
    double scoreRisco,
    double scoreFinal,

    List<ResultadoDimensao> dimensoes,
    LocalDateTime calculadoEm
) {
    /** Classificação textual para exibição no front-end. */
    public String classificacao() {
        if (scoreFinal >= 80) return "Excelente";
        if (scoreFinal >= 65) return "Muito bom";
        if (scoreFinal >= 50) return "Bom";
        if (scoreFinal >= 35) return "Regular";
        return "Baixo";
    }

    /** Resumo compacto para logs e alertas. */
    public String resumo() {
        return "[%s] Score %.1f | Desconto %.0f | Financ. %.0f | Local. %.0f | Risco %.0f | %s/%s"
            .formatted(classificacao(), scoreFinal,
                scoreDesconto, scoreFinanciamento, scoreLocalizacao, scoreRisco,
                fonte, idExterno);
    }
}
