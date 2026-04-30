package com.leilao.alerta;

import com.leilao.model.Lote;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registra cada alerta enviado — evita re-envios do mesmo lote.
 * A constraint UNIQUE (assinante_id, lote_id, canal) garante unicidade no banco.
 */
@Entity
@Table(
    name = "historico_alertas",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_historico_assinante_lote_canal",
        columnNames = {"assinante_id", "lote_id", "canal"}
    )
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinante_id", nullable = false)
    private Assinante assinante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalAlerta canal;

    @CreationTimestamp
    @Column(name = "enviado_em", nullable = false, updatable = false)
    private LocalDateTime enviadoEm;

    @Column(nullable = false)
    @Builder.Default
    private boolean sucesso = true;

    @Column(name = "erro_mensagem", columnDefinition = "TEXT")
    private String erroMensagem;
}
