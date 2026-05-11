package com.leilao.dto;

import com.leilao.model.TipoLote;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Parâmetros de busca filtrada de lotes.
 * Fix #16: @Min não funciona em BigDecimal — substituído por @DecimalMin.
 */
public record FiltroLoteDTO(

    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    String cidade,

    @Size(min = 2, max = 2, message = "Estado deve ser a UF com 2 letras (ex: SP)")
    String estado,

    TipoLote tipo,

    Boolean aceitaFinanciamento,
    Boolean aceitaFgts,

    @DecimalMin(value = "0.0", message = "Valor máximo não pode ser negativo") // Fix #16
    BigDecimal valorMaximo,

    @DecimalMin(value = "0.0", message = "Score mínimo não pode ser negativo") // Fix #16
    BigDecimal scoreMinimo,

    @Min(1) @Max(200)
    Integer limite
) {
    public int limiteEfetivo() {
        return limite != null ? limite : 50;
    }
}
