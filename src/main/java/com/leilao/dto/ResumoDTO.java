package com.leilao.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estatísticas gerais da plataforma — retornado por GET /lotes/resumo.
 */
public record ResumoDTO(
    long           totalLotes,
    long           lotesDisponiveis,
    long           lotesComScore,
    long           coletadosHoje,
    Map<String, Long> porFonte,
    Map<String, Long> porTipo,
    BigDecimal     scoreMediano,
    BigDecimal     maiorDesconto,
    LocalDateTime  ultimaColeta
) {}
