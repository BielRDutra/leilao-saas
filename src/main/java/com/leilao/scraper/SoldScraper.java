package com.leilao.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leilao.config.ScraperConfig;
import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scraper do Sold (sold.com.br).
 * Equivalente ao scrapers/sold.py do Python.
 *
 * Estratégia: API JSON interna — OkHttp puro, sem browser.
 */
@Slf4j
@Component
public class SoldScraper extends ScraperBase {

    private static final String API_URL = "https://www.sold.com.br/api/v1/lotes";

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public SoldScraper(ScraperConfig config) {
        super(config);
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String getNome() { return "sold"; }

    // ── Coleta principal ──────────────────────────────────────────────────────

    @Override
    public List<Lote> coletar() {
        List<Lote> lotes = new ArrayList<>();

        for (int pagina = 1; pagina <= config.getMaxPaginas(); pagina++) {
            log.info("[sold] Coletando página {}/{}", pagina, config.getMaxPaginas());

            HttpUrl url = HttpUrl.parse(API_URL).newBuilder()
                .addQueryParameter("categoria", "imoveis")
                .addQueryParameter("status", "ativo")
                .addQueryParameter("per_page", "50")
                .addQueryParameter("page", String.valueOf(pagina))
                .build();

            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("[sold] HTTP {} na página {}", response.code(), pagina);
                    break;
                }

                JsonNode json = mapper.readTree(response.body().string());
                JsonNode itens = json.has("data") ? json.get("data") : json.get("lotes");

                if (itens == null || itens.isEmpty()) {
                    log.info("[sold] Sem mais itens. Fim da paginação.");
                    break;
                }

                for (JsonNode item : itens) {
                    Lote lote = parsearItem(item);
                    if (lote != null) lotes.add(lote);
                }

            } catch (Exception e) {
                log.error("[sold] Erro na página {}: {}", pagina, e.getMessage());
                break;
            }

            delayHumano();
        }

        log.info("[sold] {} lotes coletados", lotes.size());
        return lotes;
    }

    // ── Parser do item JSON ───────────────────────────────────────────────────

    private Lote parsearItem(JsonNode item) {
        String idExterno = txtNode(item, "id", "lote_id");
        if (idExterno == null || idExterno.isBlank()) return null;

        BigDecimal valorLance = valorNode(item, "lance_inicial", "valor_inicial");
        if (valorLance == null) return null;

        JsonNode endNode = item.has("endereco") ? item.get("endereco") : item;
        String cidade = txtNode(endNode, "cidade");
        String estado = txtNode(endNode, "uf");

        // Condições de pagamento podem vir como array ou booleans
        boolean aceitaFin, aceitaFgts;
        if (item.has("condicoes_pagamento") && item.get("condicoes_pagamento").isArray()) {
            StringBuilder conds = new StringBuilder();
            item.get("condicoes_pagamento").forEach(c -> conds.append(c.asText().toLowerCase()));
            aceitaFin  = conds.toString().contains("financ");
            aceitaFgts = conds.toString().contains("fgts");
        } else {
            aceitaFin  = item.has("financiamento") && item.get("financiamento").asBoolean();
            aceitaFgts = item.has("fgts")          && item.get("fgts").asBoolean();
        }

        return Lote.builder()
            .fonte("sold")
            .idExterno(idExterno)
            .urlOriginal("https://www.sold.com.br/lote/" + idExterno)
            .tipo(mapearTipo(txtNode(item, "categoria")))
            .origem(OrigemLeilao.EXTRAJUDICIAL)
            .status(StatusLote.DISPONIVEL)
            .valorAvaliacao(valorNode(item, "valor_avaliacao"))
            .valorLanceInicial(valorLance)
            .aceitaFinanciamento(aceitaFin)
            .aceitaFgts(aceitaFgts)
            .logradouro(endNode.has("logradouro") ? endNode.get("logradouro").asText() : txtNode(item, "endereco_texto"))
            .bairro(txtNode(endNode, "bairro"))
            .cidade(cidade)
            .estado(estado)
            .cep(txtNode(endNode, "cep"))
            .descricao(txtNode(item, "descricao"))
            .areaM2(valorNode(item, "area"))
            .dataLeilao(parsearData(txtNode(item, "data_leilao")))
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String txtNode(JsonNode node, String... campos) {
        for (String campo : campos) {
            if (node != null && node.has(campo) && !node.get(campo).isNull())
                return node.get(campo).asText().trim();
        }
        return null;
    }

    private static BigDecimal valorNode(JsonNode node, String... campos) {
        for (String campo : campos) {
            if (node != null && node.has(campo) && !node.get(campo).isNull()) {
                String txt = node.get(campo).asText().trim();
                BigDecimal v = limparValor(txt);
                if (v != null) return v;
            }
        }
        return null;
    }

    private static LocalDateTime parsearData(String texto) {
        if (texto == null || texto.isBlank()) return null;
        for (String pattern : List.of("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd/MM/yyyy HH:mm", "dd/MM/yyyy")) {
            try {
                String t = texto.length() > pattern.length() ? texto.substring(0, pattern.length()) : texto;
                if (pattern.contains("HH")) {
                    return LocalDateTime.parse(t, DateTimeFormatter.ofPattern(pattern));
                } else {
                    return LocalDate.parse(t, DateTimeFormatter.ofPattern(pattern)).atStartOfDay();
                }
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
