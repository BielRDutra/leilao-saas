package com.leilao.score;

import com.leilao.util.Classificacao;
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
    // Fix #14: delegado para utilitário centralizado
    public String classificacao() { return Classificacao.de(scoreFinal); }

    /** Resumo compacto para logs e alertas. */
    public String resumo() {
        return "[%s] Score %.1f | Desconto %.0f | Financ. %.0f | Local. %.0f | Risco %.0f | %s/%s"
            .formatted(classificacao(), scoreFinal,
                scoreDesconto, scoreFinanciamento, scoreLocalizacao, scoreRisco,
                fonte, idExterno);
    }
}
