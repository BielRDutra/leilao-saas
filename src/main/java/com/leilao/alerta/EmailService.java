package com.leilao.alerta;

import com.leilao.model.Lote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Serviço de envio de e-mail via SMTP (Spring Mail).
 * Configurar em application.properties:
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=seu@gmail.com
 *   spring.mail.password=senha_app
 *   spring.mail.properties.mail.smtp.auth=true
 *   spring.mail.properties.mail.smtp.starttls.enable=true
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${leilao.alerta.email.remetente:noreply@leilaosmrt.com}")
    private String remetente;

    @Value("${leilao.alerta.email.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Envia o e-mail de alerta para o assinante.
     * Retorna true se enviou com sucesso.
     */
    public boolean enviarAlerta(Assinante assinante, Lote lote) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(remetente);
            helper.setTo(assinante.getEmail());
            helper.setSubject(assunto(lote));
            helper.setText(corpo(assinante, lote), true); // true = HTML

            mailSender.send(msg);
            log.info("[email] Alerta enviado → {} | lote {}", assinante.getEmail(), lote.getId());
            return true;

        } catch (Exception e) {
            log.error("[email] Falha ao enviar para {}: {}", assinante.getEmail(), e.getMessage());
            return false;
        }
    }

    // ── Helpers de conteúdo ───────────────────────────────────────────────────

    private String assunto(Lote lote) {
        return "🏠 Nova oportunidade de leilão — Score %.0f | %s/%s"
            .formatted(
                lote.getScoreOportunidade() != null ? lote.getScoreOportunidade().doubleValue() : 0,
                lote.getCidade() != null ? lote.getCidade() : "—",
                lote.getEstado() != null ? lote.getEstado() : "—"
            );
    }

    private String corpo(Assinante assinante, Lote lote) {
        String urlDetalhe = "%s/lote/%d".formatted(baseUrl, lote.getId());
        String urlOriginal = lote.getUrlOriginal();

        NumberFormat brl = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String lance      = brl.format(lote.getValorLanceInicial());
        String avaliacao  = lote.getValorAvaliacao() != null
            ? brl.format(lote.getValorAvaliacao()) : "—";
        String desconto   = lote.getDescontoPercentual() != null
            ? "%.1f%%".formatted(lote.getDescontoPercentual().doubleValue()) : "—";
        String score      = lote.getScoreOportunidade() != null
            ? "%.1f".formatted(lote.getScoreOportunidade().doubleValue()) : "—";
        String cidade     = "%s/%s".formatted(
            lote.getCidade() != null ? lote.getCidade() : "—",
            lote.getEstado() != null ? lote.getEstado() : "—"
        );
        String financ     = lote.isAceitaFinanciamento() ? "✅ Sim" : "❌ Não";
        String fgts       = lote.isAceitaFgts() ? "✅ Sim" : "❌ Não";

        String corScore = lote.getScoreOportunidade() != null
            && lote.getScoreOportunidade().compareTo(BigDecimal.valueOf(65)) >= 0
            ? "#10b981" : "#3b82f6";

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f9fafb;font-family:Arial,sans-serif">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:32px 16px">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;
                                border:1px solid #e5e7eb;overflow:hidden">

                    <!-- Header -->
                    <tr>
                      <td style="background:#534AB7;padding:24px 32px">
                        <h1 style="margin:0;color:#fff;font-size:20px">
                          🏠 Nova oportunidade de leilão
                        </h1>
                        <p style="margin:4px 0 0;color:#c7d2fe;font-size:14px">
                          Olá %s, encontramos um lote dentro do seu perfil!
                        </p>
                      </td>
                    </tr>

                    <!-- Score destaque -->
                    <tr>
                      <td style="padding:24px 32px 0">
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td style="background:#f0fdf4;border:1px solid #bbf7d0;
                                       border-radius:8px;padding:16px;text-align:center">
                              <p style="margin:0;font-size:13px;color:#6b7280">
                                Score de oportunidade
                              </p>
                              <p style="margin:4px 0 0;font-size:40px;font-weight:bold;
                                        color:%s">%s</p>
                              <p style="margin:4px 0 0;font-size:13px;color:#6b7280">
                                %s
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- Dados do lote -->
                    <tr>
                      <td style="padding:24px 32px">
                        <table width="100%%" cellpadding="8" cellspacing="0"
                               style="border-collapse:collapse">
                          %s
                          %s
                          %s
                          %s
                          %s
                          %s
                          %s
                        </table>
                      </td>
                    </tr>

                    <!-- Botões -->
                    <tr>
                      <td style="padding:0 32px 32px">
                        <table cellpadding="0" cellspacing="0">
                          <tr>
                            <td style="padding-right:12px">
                              <a href="%s"
                                 style="background:#534AB7;color:#fff;padding:12px 24px;
                                        border-radius:8px;text-decoration:none;
                                        font-size:14px;font-weight:bold;display:inline-block">
                                Ver detalhes
                              </a>
                            </td>
                            <td>
                              <a href="%s"
                                 style="background:#f3f4f6;color:#374151;padding:12px 24px;
                                        border-radius:8px;text-decoration:none;
                                        font-size:14px;display:inline-block">
                                Portal original ↗
                              </a>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f9fafb;padding:16px 32px;
                                 border-top:1px solid #e5e7eb">
                        <p style="margin:0;font-size:12px;color:#9ca3af">
                          Você recebeu este e-mail porque se cadastrou nos alertas do
                          LeilãoSmart com score mínimo de %s.
                          Para cancelar, acesse suas preferências.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(
                assinante.getNome() != null ? assinante.getNome() : "usuário",
                corScore, score, lote.getClassificacao() != null ? lote.getClassificacao() : "",
                linha("Localização", cidade),
                linha("Lance inicial", lance),
                linha("Avaliação", avaliacao),
                linha("Desconto", desconto),
                linha("Financiamento", financ),
                linha("FGTS", fgts),
                linha("Tipo", lote.getTipo() != null ? lote.getTipo().name() : "—"),
                urlDetalhe, urlOriginal,
                assinante.getScoreMinimo()
            );
    }

    private String linha(String label, String valor) {
        return """
            <tr style="border-bottom:1px solid #f3f4f6">
              <td style="font-size:13px;color:#6b7280;width:40%%">%s</td>
              <td style="font-size:14px;color:#111827;font-weight:500">%s</td>
            </tr>
            """.formatted(label, valor);
    }
}
