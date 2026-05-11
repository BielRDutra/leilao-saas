package com.leilao.alerta.dto;

import com.leilao.alerta.Assinante;
import com.leilao.model.TipoLote;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de criação de assinante.
 */
public record CriarAssinanteDTO(

    @Size(max = 100)
    String nome,

    @Email(message = "E-mail inválido")
    String email,

    @Pattern(regexp = "\\d{12,13}", message = "WhatsApp deve ter 12-13 dígitos (ex: 5511999990000)")
    String whatsapp,

    @NotNull(message = "Score mínimo é obrigatório")
    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    BigDecimal scoreMinimo,

    @Size(max = 100) String cidade,
    @Size(min = 2, max = 2) String estado,
    TipoLote tipoLote
) {
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
