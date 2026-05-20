package com.leilao.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador de coletas.
 * Equivalente ao scheduler/orquestrador.py do Python.
 *
 * @Scheduled dispara automaticamente no cron configurado em application.properties.
 * ApplicationRunner permite execução manual via argumento --executar-agora.
 */
@Slf4j
@Component
public class ScraperScheduler implements ApplicationRunner {

    private final ScraperService scraperService;

    public ScraperScheduler(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    /**
     * Coleta diária automática.
     * Horário configurado em leilao.scraper.cron (default: 06:00 BRT).
     *
     * Para alterar o horário, edite application.properties:
     *   leilao.scraper.cron=0 0 8 * * *   ← 08:00
     */
    @Scheduled(cron = "${leilao.scraper.cron}")
    public void coletaDiaria() {
        log.info("[scheduler] Coleta diária iniciada pelo agendador.");
        scraperService.executarTodos();
    }

    /**
     * Execução manual via linha de comando.
     * Uso: java -jar leilao-saas.jar --executar-agora
     *
     * Útil para testar sem esperar o cron ou para reprocessar dados.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("executar-agora")) {
            log.info("[scheduler] Execução manual solicitada (--executar-agora).");
            scraperService.executarTodos();
        } else {
            log.info("[scheduler] Aguardando próxima execução agendada.");
            log.info("[scheduler] Para executar agora: java -jar leilao-saas.jar --executar-agora");
        }
    }
}
