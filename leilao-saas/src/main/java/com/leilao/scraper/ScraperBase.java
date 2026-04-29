package com.leilao.scraper;

import com.leilao.config.ScraperConfig;
import com.leilao.model.Lote;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Classe base para todos os scrapers.
 * Equivalente ao scrapers/base.py do Python.
 *
 * Fornece:
 *  - WebDriver (Chrome headless via Selenium)
 *  - Delay humano aleatório entre requisições
 *  - Helpers de limpeza de valores monetários e áreas
 */
@Slf4j
public abstract class ScraperBase {

    protected final ScraperConfig config;
    private final Random random = new Random();

    protected ScraperBase(ScraperConfig config) {
        this.config = config;
    }

    /** Nome do portal — sobrescrever nas subclasses. */
    public abstract String getNome();

    /** Executa a coleta e retorna lista de Lotes prontos para salvar. */
    public abstract List<Lote> coletar();

    // ── WebDriver ─────────────────────────────────────────────────────────────

    /**
     * Cria um WebDriver Chrome configurado.
     * Usa WebDriverManager para baixar o chromedriver automaticamente.
     * Sempre chame driver.quit() no finally do caller.
     */
    protected WebDriver criarDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1280,900",
            "--disable-blink-features=AutomationControlled",
            // Bloqueia imagens para acelerar o scraping
            "--blink-settings=imagesEnabled=false"
        );
        options.addArguments("--lang=pt-BR");

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts()
              .pageLoadTimeout(java.time.Duration.ofMillis(config.getPageTimeoutMs()));
        return driver;
    }

    // ── Delay humano ──────────────────────────────────────────────────────────

    protected void delayHumano() {
        long delay = config.getDelayMinMs()
                + (long)(random.nextDouble() * (config.getDelayMaxMs() - config.getDelayMinMs()));
        log.debug("[{}] Aguardando {}ms...", getNome(), delay);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Helpers de parsing ────────────────────────────────────────────────────

    /**
     * Converte texto monetário brasileiro para BigDecimal.
     * Ex: "R$ 1.250.000,00" → 1250000.00
     * Retorna null se o texto for inválido.
     */
    protected static BigDecimal limparValor(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            String limpo = texto
                .replace("R$", "")
                .replace(".", "")
                .replace(",", ".")
                .replaceAll("[^\\d.]", "")
                .trim();
            if (limpo.isEmpty()) return null;
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converte texto de área para BigDecimal.
     * Ex: "120,50 m²" → 120.50
     */
    protected static BigDecimal limparArea(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            String limpo = texto
                .replace("m²", "")
                .replace("m2", "")
                .replace(",", ".")
                .replaceAll("[^\\d.]", "")
                .trim();
            if (limpo.isEmpty()) return null;
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extrai cidade e UF de um endereço no formato "..., Cidade/UF".
     * Retorna array de 2 elementos: [cidade, estado]. Nulos se não encontrar.
     */
    protected static String[] parsearCidadeEstado(String endereco) {
        if (endereco == null || !endereco.contains("/")) return new String[]{null, null};
        String[] partes = endereco.split("/");
        String estado = partes[partes.length - 1].trim().length() >= 2
            ? partes[partes.length - 1].trim().substring(0, 2).toUpperCase()
            : null;
        String cidadeParte = partes[partes.length - 2];
        String cidade = cidadeParte.contains(",")
            ? cidadeParte.substring(cidadeParte.lastIndexOf(',') + 1).trim()
            : cidadeParte.trim();
        return new String[]{cidade, estado};
    }

    /**
     * Mapeia texto de categoria para TipoLote.
     */
    protected static com.leilao.model.TipoLote mapearTipo(String categoria) {
        if (categoria == null) return com.leilao.model.TipoLote.OUTROS;
        String c = categoria.toLowerCase();
        if (c.matches(".*(residencial|apartamento|apto|casa|kitnet|quitinete).*"))
            return com.leilao.model.TipoLote.IMOVEL_RESIDENCIAL;
        if (c.matches(".*(comercial|sala|loja|galp[aã]o|industrial).*"))
            return com.leilao.model.TipoLote.IMOVEL_COMERCIAL;
        if (c.matches(".*(rural|s[ií]tio|fazenda|ch[aá]cara).*"))
            return com.leilao.model.TipoLote.IMOVEL_RURAL;
        if (c.matches(".*(ve[íi]culo|carro|moto|caminh[aã]o|autom[oó]vel).*"))
            return com.leilao.model.TipoLote.VEICULO;
        return com.leilao.model.TipoLote.OUTROS;
    }
}
