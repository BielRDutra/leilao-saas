package com.leilao.alerta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leilao.model.Lote;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Serviço de envio de mensagens WhatsApp via Z-API.
 * https://developer.z-api.io
 *
 * Configurar em application.properties:
 *   leilao.alerta.zapi.instance-id=SUA_INSTANCIA
 *   leilao.alerta.zapi.token=SEU_TOKEN
 *   leilao.alerta.zapi.security-token=SEU_SECURITY_TOKEN
 *   leilao.alerta.email.base-url=https://seusite.com
 *
 * O número de WhatsApp do assinante deve estar no formato internacional
 * sem o "+": "5511999990000"
 */
@Slf4j
@Service
public class WhatsAppService {

    private static final String ZAPI_BASE = "https://api.z-api.io/instances/%s/token/%s/send-text";

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    @Value("${leilao.alerta.zapi.instance-id:}")
    private String instanceId;

    @Value("${leilao.alerta.zapi.token:}")
    private String token;

    @Value("${leilao.alerta.zapi.security-token:}")
    private String securityToken;

    @Value("${leilao.alerta.email.base-url:http://localhost:3000}")
    private String baseUrl;

    public WhatsAppService() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Envia mensagem de alerta via WhatsApp.
     * Retorna true se enviou com sucesso.
     */
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
                .post(RequestBody.create(corpo, MediaType.parse("application/json")));

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

        } catch (Exception e) {
            log.error("[whatsapp] Falha ao enviar para {}: {}",
                assinante.getWhatsapp(), e.getMessage());
            return false;
        }
    }

    // ── Conteúdo da mensagem ──────────────────────────────────────────────────

    private String montarMensagem(Assinante assinante, Lote lote) {
        NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        String nome     = assinante.getNome() != null ? assinante.getNome() : "usuário";
        String lance    = brl.format(lote.getValorLanceInicial());
        String desconto = lote.getDescontoPercentual() != null
            ? "%.1f%% abaixo da avaliação".formatted(lote.getDescontoPercentual().doubleValue())
            : "desconto não informado";
        String score    = lote.getScoreOportunidade() != null
            ? "%.1f".formatted(lote.getScoreOportunidade().doubleValue()) : "—";
        String classif  = lote.getClassificacao() != null ? lote.getClassificacao() : "";
        String local    = "%s/%s".formatted(
            lote.getCidade()  != null ? lote.getCidade()  : "—",
            lote.getEstado()  != null ? lote.getEstado()  : "—"
        );
        String financ   = lote.isAceitaFinanciamento() ? "✅ Financiamento" : "";
        String fgts     = lote.isAceitaFgts() ? "✅ FGTS" : "";
        String tags     = "%s %s".formatted(financ, fgts).trim();
        String urlLote  = "%s/lote/%d".formatted(baseUrl, lote.getId());

        return """
            🏠 *Nova oportunidade, %s!*

            📊 Score: *%s* — _%s_
            📍 %s
            💰 Lance: *%s*
            📉 %s
            %s

            🔗 Ver detalhes:
            %s
            """.formatted(nome, score, classif, local, lance, desconto,
                tags.isBlank() ? "" : tags, urlLote).strip();
    }
}
