package com.leilao.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Entidade central — representa um lote coletado de qualquer portal de leilão.
 * Equivalente ao models/lote.py do projeto Python.
 *
 * Campos de score (preenchidos pelo MotorScore):
 *   scoreOportunidade   — 0 a 100, média ponderada das 4 dimensões
 *   scoreDesconto       — sub-score da dimensão desconto
 *   scoreFinanciamento  — sub-score da dimensão financiamento
 *   scoreLocalizacao    — sub-score da dimensão localização
 *   scoreRisco          — sub-score da dimensão risco
 */
@Entity
@Table(
    name = "lotes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_lotes_fonte_id_externo",
        columnNames = {"fonte", "id_externo"}
    ),
    indexes = {
        @Index(name = "ix_lotes_data_leilao",    columnList = "data_leilao"),
        @Index(name = "ix_lotes_cidade_estado",  columnList = "cidade, estado"),
        @Index(name = "ix_lotes_tipo_status",    columnList = "tipo, status"),
        @Index(name = "ix_lotes_score",          columnList = "score_oportunidade")
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    // ── Identificação ─────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String fonte;              // "caixa" | "superbid" | "sold" | "parque_leiloes"

    @Column(name = "id_externo", nullable = false, length = 100)
    private String idExterno;          // ID original no portal

    @Column(name = "url_original", nullable = false, columnDefinition = "TEXT")
    private String urlOriginal;

    // ── Classificação ─────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TipoLote tipo = TipoLote.OUTROS;

    @Enumerated(EnumType.STRING)
    private OrigemLeilao origem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusLote status = StatusLote.DISPONIVEL;

    // ── Valores ───────────────────────────────────────────────────────────────
    @Column(name = "valor_avaliacao", precision = 15, scale = 2)
    private BigDecimal valorAvaliacao;         // valor de mercado

    @Column(name = "valor_lance_inicial", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLanceInicial;      // lance mínimo

    @Column(name = "valor_incremento", precision = 15, scale = 2)
    private BigDecimal valorIncremento;

    // ── Financiamento ─────────────────────────────────────────────────────────
    @Column(name = "aceita_financiamento", nullable = false)
    @Builder.Default
    private boolean aceitaFinanciamento = false;

    @Column(name = "aceita_fgts", nullable = false)
    @Builder.Default
    private boolean aceitaFgts = false;

    @Column(name = "banco_financiador", length = 100)
    private String bancoFinanciador;

    // ── Localização ───────────────────────────────────────────────────────────
    @Column(length = 255)
    private String logradouro;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String estado;             // UF: "SP", "RJ", etc.

    @Column(length = 9)
    private String cep;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // ── Detalhes do bem ───────────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "area_m2", precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Column(length = 50)
    private String matricula;

    @Column(name = "debitos_conhecidos", columnDefinition = "TEXT")
    private String debitosConhecidos;

    @Column(nullable = true)
    private Boolean ocupado;          // true = imóvel ocupado (risco de desocupação)

    // ── Datas ─────────────────────────────────────────────────────────────────
    @Column(name = "data_leilao")
    private LocalDateTime dataLeilao;

    @Column(name = "data_primeiro_leilao")
    private LocalDateTime dataPrimeiroLeilao;

    @Column(name = "data_segundo_leilao")
    private LocalDateTime dataSegundoLeilao;

    // ── Score (preenchido pelo MotorScore) ────────────────────────────────────
    @Column(name = "score_oportunidade", precision = 5, scale = 2)
    private BigDecimal scoreOportunidade;

    @Column(name = "score_desconto", precision = 5, scale = 2)
    private BigDecimal scoreDesconto;

    @Column(name = "score_financiamento", precision = 5, scale = 2)
    private BigDecimal scoreFinanciamento;

    @Column(name = "score_localizacao", precision = 5, scale = 2)
    private BigDecimal scoreLocalizacao;

    @Column(name = "score_risco", precision = 5, scale = 2)
    private BigDecimal scoreRisco;

    @Column(name = "score_calculado_em")
    private LocalDateTime scoreCalculadoEm;

    // ── Controle interno ──────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "coletado_em", nullable = false, updatable = false)
    private LocalDateTime coletadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "fonte_scraper_versao", length = 20)
    private String fonteScraperVersao;

    @Column(name = "html_raw", columnDefinition = "TEXT")
    private String htmlRaw;            // HTML bruto para reprocessamento

    // ── Propriedade calculada ─────────────────────────────────────────────────

    /**
     * Desconto do lance inicial em relação ao valor de avaliação.
     * Ex: avaliação R$500k, lance R$300k → 40.00%
     * Retorna null se não houver valor de avaliação.
     */
    public BigDecimal getDescontoPercentual() {
        if (valorAvaliacao == null || valorAvaliacao.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return BigDecimal.ONE
                .subtract(valorLanceInicial.divide(valorAvaliacao, 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "Lote{id=%d, fonte='%s', tipo=%s, lance=R$%s}"
                .formatted(id, fonte, tipo, valorLanceInicial);
    }

    public Object getScoreOportunidade() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
