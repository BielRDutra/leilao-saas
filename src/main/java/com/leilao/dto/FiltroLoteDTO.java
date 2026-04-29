package com.leilao.dto;

import com.leilao.model.TipoLote;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Parâmetros de busca filtrada de lotes.
 * Todos os campos são opcionais — só filtra o que for informado.
 */
public record FiltroLoteDTO(

    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    String cidade,

    @Size(min = 2, max = 2, message = "Estado deve ser a UF com 2 letras (ex: SP)")
    String estado,

    TipoLote tipo,

    Boolean aceitaFinanciamento,
    Boolean aceitaFgts,

    @Min(value = 0, message = "Valor máximo não pode ser negativo")
    BigDecimal valorMaximo,

    @Min(value = 0, message = "Score mínimo não pode ser negativo")
    BigDecimal scoreMinimo,

    @Min(value = 1) @jakarta.validation.constraints.Max(200)
    Integer limite
) {
    /** Retorna limite com fallback para 50. */
    public int limiteEfetivo() {
        return limite != null ? limite : 50;
    }
}
