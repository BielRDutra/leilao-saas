package com.leilao.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configurações dos scrapers.
 * Lidas do application.properties via prefixo "leilao.scraper".
 */
@Configuration
@ConfigurationProperties(prefix = "leilao.scraper")
@Getter @Setter
public class ScraperConfig {

    private long   delayMinMs     = 2000;
    private long   delayMaxMs     = 5000;
    private int    maxRetries     = 3;
    private int    pageTimeoutMs  = 30000;
    private boolean headless      = true;
    private int    maxPaginas     = 5;
    private String cron           = "0 0 6 * * *";

    // UFs para coleta da Caixa
    private List<String> caixaUfs = List.of(
        "SP","RJ","MG","RS","PR","SC","BA","GO","DF","CE"
    );
}
