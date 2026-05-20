package com.leilao.alerta;

import com.leilao.model.TipoLote;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Assinante de alertas.
 * Define os critérios (score mínimo, cidade, estado, tipo)
 * e os canais (e-mail e/ou WhatsApp) de notificação.
 */
@Entity
@Table(name = "assinantes")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assinante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nome;

    @Column(length = 255)
    private String email;        // null = não recebe e-mail

    @Column(length = 20)
    private String whatsapp;     // null = não recebe WhatsApp (ex: "5511999990000")

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    // ── Critérios do alerta ────────────────────────────────────────────────────

    @Column(name = "score_minimo", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal scoreMinimo = new BigDecimal("50.0");

    @Column(length = 100)
    private String cidade;       // null = qualquer cidade

    @Column(length = 2)
    private String estado;       // null = qualquer estado (UF)

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lote", length = 30)
    private TipoLote tipoLote;   // null = qualquer tipo

    // ── Controle ──────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // ── Histórico (relação inversa) ────────────────────────────────────────────
    @OneToMany(mappedBy = "assinante", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistoricoAlerta> historico;

    // ── Utilitários ───────────────────────────────────────────────────────────

    public boolean temEmail()     { return email    != null && !email.isBlank(); }
    public boolean temWhatsApp()  { return whatsapp != null && !whatsapp.isBlank(); }

    @Override
    public String toString() {
        return "Assinante{id=%d, nome='%s', score≥%s, %s/%s}"
            .formatted(id, nome, scoreMinimo, cidade, estado);
    }
}
