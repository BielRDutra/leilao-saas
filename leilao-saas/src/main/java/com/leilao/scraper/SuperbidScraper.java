package com.leilao.scraper;

import com.leilao.config.ScraperConfig;
import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper do Superbid (superbid.net).
 * Equivalente ao scrapers/superbid.py do Python.
 *
 * Estratégia: Selenium navega a listagem → Jsoup parseia cada página de detalhe.
 */
@Slf4j
@Component
public class SuperbidScraper extends ScraperBase {

    private static final String URL_LISTAGEM =
        "https://www.superbid.net/busca?categoria=imoveis&status=ativo&ordenar=data_leilao&pagina=%d";

    private static final DateTimeFormatter FMT_COMPLETO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public SuperbidScraper(ScraperConfig config) {
        super(config);
    }

    @Override
    public String getNome() { return "superbid"; }

    // ── Coleta principal ──────────────────────────────────────────────────────

    @Override
    public List<Lote> coletar() {
        List<Lote> lotes = new ArrayList<>();
        WebDriver driver = criarDriver();

        try {
            List<String> urlsLotes = coletarUrlsListagem(driver);
            log.info("[superbid] {} URLs encontradas", urlsLotes.size());

            for (String urlLote : urlsLotes) {
                Lote lote = coletarDetalhe(driver, urlLote);
                if (lote != null) lotes.add(lote);
            }
        } finally {
            driver.quit();
        }

        log.info("[superbid] {} lotes coletados", lotes.size());
        return lotes;
    }

    // ── Listagem ──────────────────────────────────────────────────────────────

    private List<String> coletarUrlsListagem(WebDriver driver) {
        List<String> urls = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(config.getPageTimeoutMs()));

        for (int pagina = 1; pagina <= config.getMaxPaginas(); pagina++) {
            String url = URL_LISTAGEM.formatted(pagina);
            log.info("[superbid] Listagem página {}/{}", pagina, config.getMaxPaginas());

            try {
                driver.get(url);
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[data-testid='lote-card']")));
                delayHumano();

                Document doc = Jsoup.parse(driver.getPageSource());
                List<String> novas = extrairUrlsListagem(doc);

                if (novas.isEmpty()) {
                    log.info("[superbid] Sem mais lotes na página {}. Encerrando.", pagina);
                    break;
                }
                urls.addAll(novas);

            } catch (Exception e) {
                log.warn("[superbid] Falha na página {}: {}", pagina, e.getMessage());
                break;
            }
        }
        return urls;
    }

    private List<String> extrairUrlsListagem(Document doc) {
        List<String> urls = new ArrayList<>();
        for (Element a : doc.select("[data-testid='lote-card'] a[href]")) {
            String href = a.attr("abs:href");
            if (href.contains("/lote/") && !urls.contains(href)) {
                urls.add(href);
            }
        }
        return urls;
    }

    // ── Detalhe ───────────────────────────────────────────────────────────────

    private Lote coletarDetalhe(WebDriver driver, String url) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(config.getPageTimeoutMs()));
        try {
            driver.get(url);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
            delayHumano();
            return parsearDetalhe(Jsoup.parse(driver.getPageSource()), url);
        } catch (Exception e) {
            log.error("[superbid] Erro no detalhe {}: {}", url, e.getMessage());
            return null;
        }
    }

    private Lote parsearDetalhe(Document doc, String url) {
        // ID externo da URL: superbid.net/lote/12345-descricao
        String idExterno = url.replaceAll(".*/lote/([^-/]+).*", "$1");

        var valorAvaliacao = limparValor(texto(doc, "[data-testid='valor-avaliacao']"));
        var valorLance     = limparValor(texto(doc, "[data-testid='valor-lance-inicial']"));
        if (valorLance == null) return null;

        String enderecoRaw = texto(doc, "[data-testid='endereco-lote']");
        String[] cidadeEstado = parsearCidadeEstado(enderecoRaw);

        // Financiamento via tags
        boolean aceitaFin  = doc.select("[data-testid='tag-financiamento']").stream()
            .anyMatch(e -> e.text().toLowerCase().contains("financ"));
        boolean aceitaFgts = doc.select("[data-testid='tag-financiamento']").stream()
            .anyMatch(e -> e.text().toLowerCase().contains("fgts"));

        return Lote.builder()
            .fonte("superbid")
            .idExterno(idExterno)
            .urlOriginal(url)
            .tipo(mapearTipo(texto(doc, "[data-testid='categoria-lote']")))
            .origem(OrigemLeilao.EXTRAJUDICIAL)
            .status(StatusLote.DISPONIVEL)
            .valorAvaliacao(valorAvaliacao)
            .valorLanceInicial(valorLance)
            .aceitaFinanciamento(aceitaFin)
            .aceitaFgts(aceitaFgts)
            .logradouro(enderecoRaw)
            .cidade(cidadeEstado[0])
            .estado(cidadeEstado[1])
            .descricao(texto(doc, "[data-testid='descricao-lote']"))
            .areaM2(limparArea(texto(doc, "[data-testid='area-lote']")))
            .dataLeilao(parsearData(texto(doc, "[data-testid='data-leilao']")))
            .htmlRaw(doc.html())
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String texto(Document doc, String seletor) {
        Element el = doc.selectFirst(seletor);
        return el != null ? el.text().trim() : null;
    }

    private static LocalDateTime parsearData(String texto) {
        if (texto == null || texto.isBlank()) return null;
        for (DateTimeFormatter fmt : List.of(FMT_COMPLETO, FMT_DATA)) {
            try {
                return LocalDateTime.parse(texto.trim().substring(0, fmt.equals(FMT_DATA) ? 10 : 16), fmt);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
