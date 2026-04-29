package com.leilao.scraper;

import com.leilao.config.ScraperConfig;
import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper do Parque dos Leilões (parquedosleiloes.com.br).
 * Equivalente ao scrapers/parque_leiloes.py do Python.
 *
 * Leilões judiciais do TJDFT — requer Selenium (site com JS).
 * Fluxo: listagem de leilões → página de lotes de cada leilão → detalhe de cada lote.
 */
@Slf4j
@Component
public class ParqueLeiloesScraper extends ScraperBase {

    private static final String URL_BASE    = "https://www.parquedosleiloes.com.br";
    private static final String URL_LEILOES = URL_BASE + "/leiloes?searchMode=normal&page=%d";
    private static final int    MAX_LEILOES = 20;

    // Regex para extrair valores do texto do edital
    private static final Pattern RE_AVALIACAO = Pattern.compile(
        "avalia[çc][aã]o[\\s:\\-]*R?\\$?\\s*([\\d.,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_LANCE_1PRACA = Pattern.compile(
        "1[ªa°.]\\s*pra[çc]a[\\s\\-:R$]*\\s*([\\d.,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_LANCE_MIN = Pattern.compile(
        "lance\\s+m[íi]nimo[\\s\\-:R$]*\\s*([\\d.,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RE_LEILAO_ID = Pattern.compile(".*/leilao/(\\d+)/?$");
    private static final Pattern RE_ENDERECO  = Pattern.compile(
        "((?:Rua|Av\\.|Avenida|QD|Quadra|SQN|SQS|SHIN|SHIS)[^,\\n]{5,80})");

    private static final Map<String, String> MESES = Map.ofEntries(
        Map.entry("janeiro","01"), Map.entry("fevereiro","02"),
        Map.entry("março","03"),   Map.entry("marco","03"),
        Map.entry("abril","04"),   Map.entry("maio","05"),
        Map.entry("junho","06"),   Map.entry("julho","07"),
        Map.entry("agosto","08"),  Map.entry("setembro","09"),
        Map.entry("outubro","10"), Map.entry("novembro","11"),
        Map.entry("dezembro","12")
    );

    public ParqueLeiloesScraper(ScraperConfig config) {
        super(config);
    }

    @Override
    public String getNome() { return "parque_leiloes"; }

    // ── Coleta principal ──────────────────────────────────────────────────────

    @Override
    public List<Lote> coletar() {
        List<Lote> lotes     = new ArrayList<>();
        List<String> urlsLeiloes = new ArrayList<>();
        WebDriver driver     = criarDriver();

        try {
            // Etapa 1: coletar URLs dos leilões ativos
            urlsLeiloes = coletarUrlsLeiloes(driver);
            log.info("[parque_leiloes] {} leilões encontrados", urlsLeiloes.size());

            // Etapa 2: coletar lotes de cada leilão
            for (String urlLeilao : urlsLeiloes) {
                List<Lote> lotesDo = coletarLotesDoLeilao(driver, urlLeilao);
                lotes.addAll(lotesDo);
            }
        } finally {
            driver.quit();
        }

        log.info("[parque_leiloes] {} lotes coletados no total", lotes.size());
        return lotes;
    }

    // ── Etapa 1: listagem de leilões ──────────────────────────────────────────

    private List<String> coletarUrlsLeiloes(WebDriver driver) {
        List<String> urls = new ArrayList<>();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(config.getPageTimeoutMs()));

        for (int pagina = 1; pagina <= config.getMaxPaginas(); pagina++) {
            String url = URL_LEILOES.formatted(pagina);
            log.info("[parque_leiloes] Listagem página {}", pagina);

            try {
                driver.get(url);
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector(".leilao-card, .auction-card, article")));
                delayHumano();

                Document doc   = Jsoup.parse(driver.getPageSource());
                List<String> novas = extrairUrlsLeiloes(doc);

                if (novas.isEmpty()) break;
                urls.addAll(novas);
                if (urls.size() >= MAX_LEILOES) break;

            } catch (Exception e) {
                log.warn("[parque_leiloes] Falha na listagem pág {}: {}", pagina, e.getMessage());
                break;
            }
        }
        return urls;
    }

    private List<String> extrairUrlsLeiloes(Document doc) {
        List<String> urls = new ArrayList<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            Matcher m   = RE_LEILAO_ID.matcher(href);
            if (m.matches()) {
                String url = href.startsWith("http") ? href : URL_BASE + href;
                if (!urls.contains(url)) urls.add(url);
            }
        }
        return urls;
    }

    // ── Etapa 2: lotes de um leilão ───────────────────────────────────────────

    private List<Lote> coletarLotesDoLeilao(WebDriver driver, String urlLeilao) {
        String urlLotes = urlLeilao.replaceAll("/?$", "") + "/lotes";
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(config.getPageTimeoutMs()));

        try {
            driver.get(urlLotes);
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(".lote-card, .lot-item, [data-lote], .lote, article")));
            delayHumano();

            Document doc       = Jsoup.parse(driver.getPageSource());
            List<Lote> lotes   = parsearLotes(doc, urlLeilao);
            log.info("[parque_leiloes]   {} lotes em {}", lotes.size(), urlLeilao);
            return lotes;

        } catch (Exception e) {
            log.warn("[parque_leiloes] Falha ao coletar lotes de {}: {}", urlLeilao, e.getMessage());
            return List.of();
        }
    }

    // ── Parser dos lotes ──────────────────────────────────────────────────────

    private List<Lote> parsearLotes(Document doc, String urlLeilao) {
        // ID do leilão extraído da URL
        Matcher m = RE_LEILAO_ID.matcher(urlLeilao);
        String idLeilao = m.find() ? m.group(1) : urlLeilao.replaceAll("\\D", "");

        // Data do leilão (cabeçalho da página)
        LocalDateTime dataLeilao = extrairDataPagina(doc);

        // Tenta vários seletores de card — o portal usa classes variadas
        Elements cards = doc.select(".lote-card");
        if (cards.isEmpty()) cards = doc.select(".lot-item");
        if (cards.isEmpty()) cards = doc.select("[data-lote]");
        if (cards.isEmpty()) cards = doc.select("article.lote");

        List<Lote> lotes = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            Lote lote = parsearCard(cards.get(i), idLeilao, i + 1, dataLeilao, urlLeilao);
            if (lote != null) lotes.add(lote);
        }
        return lotes;
    }

    private Lote parsearCard(Element card, String idLeilao, int numLote,
                             LocalDateTime dataLeilao, String urlLeilao) {
        String texto = card.wholeText().replaceAll("\\s+", " ").trim();

        String idExterno     = idLeilao + "-" + numLote;
        BigDecimal avaliacao = extrairAvaliacao(texto);
        BigDecimal lance     = extrairLance(card, texto);

        // Fallback: 60% da avaliação (mínimo judicial padrão)
        if (lance == null && avaliacao != null) {
            lance = avaliacao.multiply(new BigDecimal("0.60"));
        }
        if (lance == null) return null;

        String[] cidadeEstado = parsearCidadeEstadoJudicial(texto);
        String endereco       = extrairEndereco(card, texto);

        return Lote.builder()
            .fonte("parque_leiloes")
            .idExterno(idExterno)
            .urlOriginal(urlLeilao)
            .tipo(mapearTipo(texto))
            .origem(OrigemLeilao.JUDICIAL)
            .status(StatusLote.DISPONIVEL)
            .valorAvaliacao(avaliacao)
            .valorLanceInicial(lance)
            .aceitaFinanciamento(false)   // leilões judiciais raramente aceitam
            .aceitaFgts(false)
            .logradouro(endereco)
            .cidade(cidadeEstado[0] != null ? cidadeEstado[0] : "Brasília")
            .estado(cidadeEstado[1] != null ? cidadeEstado[1] : "DF")
            .descricao(texto.length() > 500 ? texto.substring(0, 500) : texto)
            .dataLeilao(dataLeilao)
            .dataPrimeiroLeilao(dataLeilao)
            .build();
    }

    // ── Helpers de extração ───────────────────────────────────────────────────

    private BigDecimal extrairAvaliacao(String texto) {
        Matcher m = RE_AVALIACAO.matcher(texto);
        return m.find() ? limparValor(m.group(1)) : null;
    }

    private BigDecimal extrairLance(Element card, String texto) {
        // Tenta seletor direto no card
        Element el = card.selectFirst(".valor-lance, .lance-minimo, [data-lance]");
        if (el != null) return limparValor(el.text());

        // Regex 1ª praça
        Matcher m1 = RE_LANCE_1PRACA.matcher(texto);
        if (m1.find()) return limparValor(m1.group(1));

        // Regex lance mínimo
        Matcher m2 = RE_LANCE_MIN.matcher(texto);
        if (m2.find()) return limparValor(m2.group(1));

        return null;
    }

    private String extrairEndereco(Element card, String texto) {
        Element el = card.selectFirst(".endereco, .address, [data-endereco]");
        if (el != null) return el.text().trim();
        Matcher m = RE_ENDERECO.matcher(texto);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String[] parsearCidadeEstadoJudicial(String texto) {
        Pattern p = Pattern.compile("([A-ZÁÉÍÓÚ][a-záéíóú\\s]+)[\\s/\\-–]+([A-Z]{2})\\b");
        Matcher m = p.matcher(texto);
        if (m.find()) return new String[]{m.group(1).trim(), m.group(2)};
        return new String[]{"Brasília", "DF"};
    }

    private LocalDateTime extrairDataPagina(Document doc) {
        for (String sel : List.of("h1", "h2", ".data-leilao", ".auction-date", ".leilao-data")) {
            Element el = doc.selectFirst(sel);
            if (el != null) {
                LocalDateTime dt = parsearDataPtBr(el.text());
                if (dt != null) return dt;
            }
        }
        return null;
    }

    private static LocalDateTime parsearDataPtBr(String texto) {
        if (texto == null || texto.isBlank()) return null;

        // Formato numérico: "27/04/2026" ou "27/04/2026 14:30"
        for (String pattern : List.of("dd/MM/yyyy HH:mm", "dd/MM/yyyy")) {
            try {
                String t = texto.trim();
                if (t.length() >= pattern.length()) t = t.substring(0, pattern.length());
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {}
        }

        // Formato extenso: "27 abril 2026"
        String normalizado = texto.toLowerCase();
        for (Map.Entry<String, String> e : MESES.entrySet()) {
            normalizado = normalizado.replace(e.getKey(), e.getValue());
        }
        Pattern p = Pattern.compile("(\\d{1,2})\\s+(\\d{2})\\s+(\\d{4})");
        Matcher m = p.matcher(normalizado);
        if (m.find()) {
            try {
                return LocalDateTime.of(
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(1)),
                    0, 0);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
