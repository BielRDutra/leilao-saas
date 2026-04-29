package com.leilao.score;

import com.leilao.config.ScoreConfig;
import com.leilao.model.Lote;
import com.leilao.repository.LoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Motor de score de oportunidade.
 * Equivalente ao score/motor.py do Python.
 *
 * Orquestra as 4 dimensões, calcula a média ponderada e persiste
 * os resultados nas colunas score_* da tabela lotes.
 */
@Slf4j
@Service
public class MotorScore {

    private final ScoreConfig config;
    private final LoteRepository loteRepository;

    private final DimensoesScore.ScoreDesconto      dimDesconto;
    private final DimensoesScore.ScoreFinanciamento dimFinanciamento;
    private final DimensoesScore.ScoreLocalizacao   dimLocalizacao;
    private final DimensoesScore.ScoreRisco         dimRisco;

    public MotorScore(ScoreConfig config, LoteRepository loteRepository) {
        this.config         = config;
        this.loteRepository = loteRepository;

        this.dimDesconto      = new DimensoesScore.ScoreDesconto(config);
        this.dimFinanciamento = new DimensoesScore.ScoreFinanciamento(config);
        this.dimLocalizacao   = new DimensoesScore.ScoreLocalizacao(config);
        this.dimRisco         = new DimensoesScore.ScoreRisco(config);
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Calcula o score de um lote SEM salvar no banco.
     * Útil para testes, preview e cálculo em memória.
     */
    public ResultadoScore calcular(Lote lote) {
        ResultadoDimensao dDesc  = dimDesconto.calcular(lote);
        ResultadoDimensao dFin   = dimFinanciamento.calcular(lote);
        ResultadoDimensao dLoc   = dimLocalizacao.calcular(lote);
        ResultadoDimensao dRisco = dimRisco.calcular(lote);

        double scoreFinal = dDesc.valor()  * config.getPesoDesconto()
            + dFin.valor()   * config.getPesoFinanciamento()
            + dLoc.valor()   * config.getPesoLocalizacao()
            + dRisco.valor() * config.getPesoRisco();

        scoreFinal = Math.round(scoreFinal * 100.0) / 100.0;

        return new ResultadoScore(
            lote.getId(),
            lote.getFonte(),
            lote.getIdExterno(),
            dDesc.valor(),
            dFin.valor(),
            dLoc.valor(),
            dRisco.valor(),
            scoreFinal,
            List.of(dDesc, dFin, dLoc, dRisco),
            LocalDateTime.now()
        );
    }

    /**
     * Calcula e persiste o score nas colunas do banco.
     * Não faz commit — o chamador controla a transação.
     */
    public ResultadoScore calcularESalvar(Lote lote) {
        ResultadoScore resultado = calcular(lote);

        lote.setScoreOportunidade(bd(resultado.scoreFinal()));
        lote.setScoreDesconto(bd(resultado.scoreDesconto()));
        lote.setScoreFinanciamento(bd(resultado.scoreFinanciamento()));
        lote.setScoreLocalizacao(bd(resultado.scoreLocalizacao()));
        lote.setScoreRisco(bd(resultado.scoreRisco()));
        lote.setScoreCalculadoEm(resultado.calculadoEm());

        loteRepository.save(lote);
        log.debug("[score] {}", resultado.resumo());
        return resultado;
    }

    /**
     * Calcula o score de todos os lotes sem pontuação (batch pós-coleta).
     * Retorna o número de lotes processados.
     */
    @Transactional
    public int processarPendentes() {
        List<Lote> lotes = loteRepository.findByScoreOportunidadeIsNull();
        if (lotes.isEmpty()) {
            log.info("[score] Nenhum lote pendente.");
            return 0;
        }

        log.info("[score] Calculando score de {} lotes...", lotes.size());
        int processados = 0;

        for (Lote lote : lotes) {
            try {
                calcularESalvar(lote);
                processados++;
            } catch (Exception e) {
                log.error("[score] Erro no lote {}/{}: {}",
                    lote.getFonte(), lote.getIdExterno(), e.getMessage());
            }
        }

        log.info("[score] Score calculado para {}/{} lotes.", processados, lotes.size());
        return processados;
    }

    /**
     * Força o recálculo de TODOS os lotes.
     * Usar quando os pesos do score forem alterados.
     */
    @Transactional
    public int recalcularTodos() {
        List<Lote> lotes = loteRepository.findAll();
        log.warn("[score] Recalculando score de {} lotes (forçado).", lotes.size());

        int processados = 0;
        for (Lote lote : lotes) {
            try {
                calcularESalvar(lote);
                processados++;
            } catch (Exception e) {
                log.error("[score] Erro no lote {}: {}", lote.getId(), e.getMessage());
            }
        }
        return processados;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static BigDecimal bd(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
