package com.leilao.scraper;

import com.leilao.config.ScraperConfig;
import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scraper da Caixa Econômica Federal.
 * Equivalente ao scrapers/caixa.py do Python.
 *
 * Estratégia: download de CSV público por UF (sem browser).
 * URL: https://venda-imoveis.caixa.gov.br/listaweb/Lista_imoveis_{UF}.csv
 */
@Slf4j
@Component
public class CaixaScraper extends ScraperBase {

    private static final String URL_CSV =
        "https://venda-imoveis.caixa.gov.br/listaweb/Lista_imoveis_%s.csv";
    private static final String URL_DETALHE =
        "https://venda-imoveis.caixa.gov.br/sistema/detalhe-imovel.asp?hdnOrigem=1&hdnSeqImovel=%s";

    private final OkHttpClient httpClient;

    public CaixaScraper(ScraperConfig config) {
        super(config);
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();
    }

    @Override
    public String getNome() { return "caixa"; }

    // ── Coleta principal ──────────────────────────────────────────────────────

    @Override
    public List<Lote> coletar() {
        List<Lote> lotes = new ArrayList<>();

        for (String uf : config.getCaixaUfs()) {
            log.info("[caixa] Coletando CSV — {}", uf);
            List<Lote> lotesDaUf = coletarCsvUf(uf);
            lotes.addAll(lotesDaUf);
            delayHumano();
        }

        log.info("[caixa] Total coletado: {} lotes em {} estados",
            lotes.size(), config.getCaixaUfs().size());
        return lotes;
    }

    // ── Download e parse do CSV ───────────────────────────────────────────────

    private List<Lote> coletarCsvUf(String uf) {
        String url = URL_CSV.formatted(uf);
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0")
            .header("Referer", "https://venda-imoveis.caixa.gov.br")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("[caixa] Falha no CSV de {}: HTTP {}", uf, response.code());
                return List.of();
            }
            // CSV da Caixa vem em Latin-1 (ISO-8859-1)
            byte[] bytes = response.body().bytes();
            String conteudo = new String(bytes, Charset.forName("ISO-8859-1"));
            return parsearCsv(conteudo, uf);

        } catch (Exception e) {
            log.error("[caixa] Erro ao baixar CSV de {}: {}", uf, e.getMessage());
            return List.of();
        }
    }

    private List<Lote> parsearCsv(String conteudo, String uf) {
        List<Lote> lotes = new ArrayList<>();
        boolean primeiraLinha = true;

        try (BufferedReader reader = new BufferedReader(new StringReader(conteudo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (primeiraLinha) { primeiraLinha = false; continue; } // pula cabeçalho
                String[] cols = linha.split(";", -1);
                if (cols.length < 9) continue;

                Lote lote = linhaParaLote(cols, uf);
                if (lote != null) lotes.add(lote);
            }
        } catch (Exception e) {
            log.error("[caixa] Erro ao parsear CSV de {}: {}", uf, e.getMessage());
        }

        log.info("[caixa] {}: {} lotes no CSV", uf, lotes.size());
        return lotes;
    }

    // ── Parser de linha ───────────────────────────────────────────────────────

    private Lote linhaParaLote(String[] cols, String ufFallback) {
        String idExterno = col(cols, 0);
        if (idExterno.isBlank() || idExterno.equalsIgnoreCase("N_Imovel")) return null;

        var valorAvaliacao = limparValor(col(cols, 7));
        var valorMinimo    = limparValor(col(cols, 8));
        if (valorMinimo == null) return null;

        String descricao        = col(cols, 9);
        String descricaoExtra   = col(cols, 12);
        String textoCompleto    = (descricao + " " + descricaoExtra).toLowerCase();

        boolean aceitaFin  = textoCompleto.matches(".*(financ|fgts).*");
        boolean aceitaFgts = textoCompleto.contains("fgts");

        String uf  = col(cols, 4).isBlank() ? ufFallback : col(cols, 4).trim();
        String urlRaw = col(cols, 11).trim();
        String url = urlRaw.startsWith("http") ? urlRaw : URL_DETALHE.formatted(idExterno);

        return Lote.builder()
            .fonte("caixa")
            .idExterno(idExterno)
            .urlOriginal(url)
            .tipo(mapearTipo(col(cols, 6)))
            .origem(OrigemLeilao.EXTRAJUDICIAL)
            .status(StatusLote.DISPONIVEL)
            .valorAvaliacao(valorAvaliacao)
            .valorLanceInicial(valorMinimo)
            .aceitaFinanciamento(aceitaFin)
            .aceitaFgts(aceitaFgts)
            .bancoFinanciador(aceitaFin ? "Caixa Econômica Federal" : null)
            .logradouro(col(cols, 1))
            .bairro(col(cols, 2))
            .cidade(col(cols, 3))
            .estado(uf)
            .cep(col(cols, 5))
            .descricao(descricao.isBlank() ? descricaoExtra : descricao)
            .build();
    }

    private static String col(String[] cols, int i) {
        return (i < cols.length) ? cols[i].trim() : "";
    }
}
