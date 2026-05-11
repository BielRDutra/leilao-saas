package com.leilao.alerta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leilao.model.Lote;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Envio de mensagens WhatsApp via Z-API.
 *
 * Fix #6: construtor padrão removido. O Spring injeta os @Value corretamente
 * via injeção de campos quando NÃO há construtor que interfira no ciclo de vida.
 * OkHttpClient e ObjectMapper são criados em @PostConstruct após injeção completa.
 */
@Slf4j
@Service
public class WhatsAppService {

    private static final String ZAPI_BASE =
        "https://api.z-api.io/instances/%s/token/%s/send-text";

    @Value("${leilao.alerta.zapi.instance-id:}")
    private String instanceId;

    @Value("${leilao.alerta.zapi.token:}")
    private String token;

    @Value("${leilao.alerta.zapi.security-token:}")
    private String securityToken;

    @Value("${leilao.alerta.email.base-url:http://localhost:3000}")
    private String baseUrl;

    private OkHttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    // Fix #6: @PostConstruct garante que @Value já foram injetados antes da inicialização
    @jakarta.annotation.PostConstruct
    void init() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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

    private String montarMensagem(Assinante assinante, Lote lote) {
        NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        String nome    = assinante.getNome() != null ? assinante.getNome() : "usuário";
        String lance   = brl.format(lote.getValorLanceInicial());
        String desconto = lote.getDescontoPercentual() != null
            ? "%.1f%% abaixo da avaliação".formatted(lote.getDescontoPercentual().doubleValue())
            : "desconto não informado";
        String score   = lote.getScoreOportunidade() != null
            ? "%.1f".formatted(lote.getScoreOportunidade().doubleValue()) : "—";
        String classif = lote.getScoreOportunidade() != null
            ? com.leilao.util.Classificacao.de(lote.getScoreOportunidade()) : ""; // Fix #14
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
