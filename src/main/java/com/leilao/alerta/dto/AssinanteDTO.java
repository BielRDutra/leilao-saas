package com.leilao.alerta.dto;

import com.leilao.alerta.Assinante;
import com.leilao.model.TipoLote;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de criação/atualização de assinante.
 */
public record CriarAssinanteDTO(

    @Size(max = 100)
    String nome,

    @Email(message = "E-mail inválido")
    String email,

    @Pattern(regexp = "\\d{12,13}", message = "WhatsApp deve ter 12-13 dígitos (ex: 5511999990000)")
    String whatsapp,

    @NotNull(message = "Score mínimo é obrigatório")
    @DecimalMin(value = "0.0")  @DecimalMax(value = "100.0")
    BigDecimal scoreMinimo,

    @Size(max = 100) String cidade,
    @Size(min = 2, max = 2) String estado,
    TipoLote tipoLote
) {
    /** Converte o DTO para a entidade Assinante. */
    public Assinante toEntity() {
        return Assinante.builder()
            .nome(nome)
            .email(email)
            .whatsapp(whatsapp)
            .scoreMinimo(scoreMinimo)
            .cidade(cidade)
            .estado(estado != null ? estado.toUpperCase() : null)
            .tipoLote(tipoLote)
            .ativo(true)
            .build();
    }
}

/**
 * DTO de resposta — representa um assinante sem dados sensíveis completos.
 */
record AssinanteRespostaDTO(
    Long          id,
    String        nome,
    String        email,        // mascarado: jo**@gmail.com
    String        whatsapp,     // mascarado: 55119****0000
    boolean       ativo,
    BigDecimal    scoreMinimo,
    String        cidade,
    String        estado,
    TipoLote      tipoLote,
    LocalDateTime criadoEm
) {
    static AssinanteRespostaDTO from(Assinante a) {
        return new AssinanteRespostaDTO(
            a.getId(), a.getNome(),
            mascararEmail(a.getEmail()),
            mascararWhatsApp(a.getWhatsapp()),
            a.isAtivo(), a.getScoreMinimo(),
            a.getCidade(), a.getEstado(), a.getTipoLote(),
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
