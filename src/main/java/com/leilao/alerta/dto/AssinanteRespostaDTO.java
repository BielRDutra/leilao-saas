package com.leilao.alerta.dto;

import com.leilao.alerta.Assinante;
import com.leilao.model.TipoLote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta de assinante com dados sensíveis mascarados.
 *
 * Fix #5: movido para arquivo próprio com visibilidade public.
 * Antes estava package-private no mesmo arquivo de CriarAssinanteDTO,
 * o que impedia seu uso no AlertaController.
 */
public record AssinanteRespostaDTO(
    Long          id,
    String        nome,
    String        email,      // mascarado: jo**@gmail.com
    String        whatsapp,   // mascarado: 5511****0000
    boolean       ativo,
    BigDecimal    scoreMinimo,
    String        cidade,
    String        estado,
    TipoLote      tipoLote,
    LocalDateTime criadoEm
) {
    public static AssinanteRespostaDTO from(Assinante a) {
        return new AssinanteRespostaDTO(
            a.getId(),
            a.getNome(),
            mascararEmail(a.getEmail()),
            mascararWhatsApp(a.getWhatsapp()),
            a.isAtivo(),
            a.getScoreMinimo(),
            a.getCidade(),
            a.getEstado(),
            a.getTipoLote(),
            a.getCriadoEm()
        );
    }

    private static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        int at = email.indexOf('@');
        if (at <= 2) return "**" + email.substring(at);
        return email.substring(0, 2) + "**" + email.substring(at);
    }

    private static String mascararWhatsApp(String phone) {
        if (phone == null || phone.length() < 8) return null;
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }
}
