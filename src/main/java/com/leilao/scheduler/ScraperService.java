package com.leilao.scheduler;

import com.leilao.alerta.AlertaService;
import com.leilao.model.Lote;
import com.leilao.repository.LoteRepository;
import com.leilao.score.MotorScore;
import com.leilao.scraper.ScraperBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Orquestra coleta, persistência, score e alertas.
 *
 * Fix #1:  AlertaService adicionado ao construtor (era declarado mas nunca injetado).
 * Fix #10: @RequiredArgsConstructor restaurado — elimina construtor manual incompleto.
 */
@Slf4j
@Service
@RequiredArgsConstructor // Fix #10: Lombok gera construtor com TODOS os campos final
public class ScraperService {

    private final List<ScraperBase> scrapers;
    private final LoteRepository    loteRepository;
    private final MotorScore        motorScore;
    private final AlertaService     alertaService; // Fix #1: agora injetado corretamente

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
                totalSalvos    += salvarLotes(lotes);
                log.info("  ✓ {} — {} coletados, {} salvos",
                    scraper.getNome(), lotes.size(), totalSalvos);
            } catch (Exception e) {
                log.error("  ✗ Scraper {} falhou: {}", scraper.getNome(), e.getMessage(), e);
            }
        }

        log.info("── Calculando scores de oportunidade...");
        int pontuados = motorScore.processarPendentes();

        log.info("── Processando alertas de score...");
        int alertasEnviados = alertaService.processarAlertasDiarios();

        log.info("══════════════════════════════════════════");
        log.info("  Coleta finalizada");
        log.info("  Coletados : {}", totalColetados);
        log.info("  Salvos    : {}", totalSalvos);
        log.info("  Pontuados : {}", pontuados);
        log.info("  Alertas   : {}", alertasEnviados);
        log.info("  Por fonte : {}", resumoPorFonte());
        log.info("══════════════════════════════════════════");
    }

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
        return sb.append("}").toString();
    }
}
