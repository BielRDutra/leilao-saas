package com.leilao.alerta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leilao.model.Lote;
import com.leilao.util.Classificacao;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppService {

    private static final String ZAPI_BASE =
        "https://api.z-api.io/instances/%s/token/%s/send-text";

    private final String instanceId;
    private final String token;
    private final String securityToken;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    public WhatsAppService(
            @Value("${leilao.alerta.zapi.instance-id:}") String instanceId,
            @Value("${leilao.alerta.zapi.token:}") String token,
            @Value("${leilao.alerta.zapi.security-token:}") String securityToken,
            @Value("${leilao.alerta.email.base-url:http://localhost:3000}") String baseUrl,
            ObjectMapper mapper) {
        this.instanceId = instanceId;
        this.token = token;
        this.securityToken = securityToken;
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    public boolean enviarAlerta(Assinante assinante, Lote lote) {
        if (instanceId.isBlank() || token.isBlank()) {
            log.warn("[whatsapp] Z-API não configurada. Ignorando envio para {}.",
                assinante.getWhatsapp());
            return false;
        }

        try {
            String mensagem = montarMensagem(assinante, lote);
            String url      = ZAPI_BASE.formatted(instanceId, token);

            String corpo = mapper.writeValueAsString(Map.of(
                "phone",   assinante.getWhatsapp(),
                "message", mensagem
            ));

            Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(corpo, MediaType.get("application/json; charset=utf-8")));

            if (!securityToken.isBlank()) {
                reqBuilder.header("Client-Token", securityToken);
            }

            try (Response resp = httpClient.newCall(reqBuilder.build()).execute()) {
                if (resp.isSuccessful()) {
                    log.info("[whatsapp] Alerta enviado → {} | lote {}",
                        assinante.getWhatsapp(), lote.getId());
                    return true;
                }
                log.error("[whatsapp] HTTP {} para {}: {}",
                    resp.code(), assinante.getWhatsapp(),
                    resp.body() != null ? resp.body().string() : "");
                return false;
            }
        } catch (IOException e) {
            log.error("[whatsapp] Falha ao enviar para {}", assinante.getWhatsapp(), e);
            return false;
        }
    }

    private String montarMensagem(Assinante assinante, Lote lote) {
        NumberFormat brl = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

        String nome    = assinante.getNome() != null ? assinante.getNome() : "usuário";
        String lance   = brl.format(lote.getValorLanceInicial());
        String desconto = lote.getDescontoPercentual() != null
            ? "%.1f%% abaixo da avaliação".formatted(lote.getDescontoPercentual().doubleValue())
            : "desconto não informado";
        String score   = lote.getScoreOportunidade() != null
            ? "%.1f".formatted(lote.getScoreOportunidade().doubleValue()) : "—";
        String classif = lote.getScoreOportunidade() != null
            ? Classificacao.de(lote.getScoreOportunidade()) : "";
        String local   = "%s/%s".formatted(
            lote.getCidade() != null ? lote.getCidade() : "—",
            lote.getEstado() != null ? lote.getEstado() : "—"
        );
        String financ  = lote.isAceitaFinanciamento() ? "✅ Financiamento" : "";
        String fgts    = lote.isAceitaFgts()          ? "✅ FGTS"         : "";
        String tags    = (financ + " " + fgts).trim();
        String urlLote = "%s/lote/%d".formatted(baseUrl, lote.getId());

        // Fix #12: template string limpo sem misturar %% de printf com variáveis Java
        StringBuilder sb = new StringBuilder();
        sb.append("🏠 *Nova oportunidade, ").append(nome).append("!*\n\n");
        sb.append("📊 Score: *").append(score).append("* — _").append(classif).append("_\n");
        sb.append("📍 ").append(local).append("\n");
        sb.append("💰 Lance: *").append(lance).append("*\n");
        sb.append("📉 ").append(desconto).append("\n");
        if (!tags.isBlank()) sb.append(tags).append("\n");
        sb.append("\n🔗 Ver detalhes:\n").append(urlLote);
        return sb.toString();
    }
}
