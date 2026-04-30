package com.leilao.scheduler;

import com.leilao.model.Lote;
import com.leilao.repository.LoteRepository;
import com.leilao.scraper.ScraperBase;
import com.leilao.alerta.AlertaService;
import com.leilao.score.MotorScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço que orquestra a coleta, persistência e score.
 * Separa a lógica de negócio do agendamento (@Scheduled).
 * Equivalente à função executar_todos_scrapers() do Python.
 */
@Slf4j
@Service
public class ScraperService {

    private final List<ScraperBase> scrapers;
    private final LoteRepository    loteRepository;
    private final MotorScore        motorScore;
    private final AlertaService     alertaService;

    public ScraperService(List<ScraperBase> scrapers,
                          LoteRepository loteRepository,
                          MotorScore motorScore) {
        this.scrapers       = scrapers;
        this.loteRepository = loteRepository;
        this.motorScore     = motorScore;
    }

    /**
     * Executa todos os scrapers registrados, salva os lotes
     * e dispara o cálculo de score para os novos registros.
     */
    public void executarTodos() {
        log.info("══════════════════════════════════════════");
        log.info("  Coleta iniciada — {} scrapers ativos", scrapers.size());
        log.info("══════════════════════════════════════════");

        int totalColetados = 0;
        int totalSalvos    = 0;

        for (ScraperBase scraper : scrapers) {
            log.info("▶ Executando: {}", scraper.getNome());
            try {
                List<Lote> lotes = scraper.coletar();
                totalColetados += lotes.size();

                int salvos = salvarLotes(lotes);
                totalSalvos += salvos;

                log.info("  ✓ {} — {} coletados, {} salvos",
                    scraper.getNome(), lotes.size(), salvos);

            } catch (Exception e) {
                log.error("  ✗ Scraper {} falhou: {}", scraper.getNome(), e.getMessage(), e);
            }
        }

        // Fase 2: calcular score dos lotes sem pontuação
        log.info("── Calculando scores de oportunidade...");
        int pontuados = motorScore.processarPendentes();

        // Resumo final
        // Fase 3: disparar alertas para os lotes pontuados
        log.info("── Processando alertas de score...");
        int alertasEnviados = alertaService.processarAlertasDiarios();
        log.info("  Alertas enviados: {}", alertasEnviados);

        log.info("══════════════════════════════════════════");
        log.info("  Coleta finalizada");
        log.info("  Coletados : {}", totalColetados);
        log.info("  Salvos    : {}", totalSalvos);
        log.info("  Pontuados : {}", pontuados);
        log.info("  Por fonte : {}", resumoPorFonte());
        log.info("══════════════════════════════════════════");
    }

    /**
     * Salva uma lista de lotes usando upsert (sem duplicatas).
     * Retorna a quantidade de lotes processados com sucesso.
     */
    @Transactional
    public int salvarLotes(List<Lote> lotes) {
        int salvos = 0;
        for (Lote lote : lotes) {
            try {
                upsert(lote);
                salvos++;
            } catch (Exception e) {
                log.error("Erro ao salvar lote {}/{}: {}",
                    lote.getFonte(), lote.getIdExterno(), e.getMessage());
            }
        }
        return salvos;
    }

    /**
     * Upsert: insere se não existir, atualiza lance e status se já existir.
     * Equivalente ao salvar_ou_atualizar() do Python.
     */
    private void upsert(Lote lote) {
        Optional<Lote> existente = loteRepository
            .findByFonteAndIdExterno(lote.getFonte(), lote.getIdExterno());

        if (existente.isPresent()) {
            Lote l = existente.get();
            l.setValorLanceInicial(lote.getValorLanceInicial());
            l.setStatus(lote.getStatus());
            l.setDataLeilao(lote.getDataLeilao());
            loteRepository.save(l);
        } else {
            loteRepository.save(lote);
        }
    }

    private String resumoPorFonte() {
        StringBuilder sb = new StringBuilder("{");
        loteRepository.contarPorFonte()
            .forEach(row -> sb.append(row[0]).append("=").append(row[1]).append(", "));
        if (sb.length() > 1) sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }
}
