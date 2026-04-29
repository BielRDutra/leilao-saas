package com.leilao.repository;

import com.leilao.model.Lote;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para a entidade Lote.
 * Equivalente ao db/repositorio_lotes.py do projeto Python.
 *
 * Spring Data gera as implementações automaticamente em tempo de execução.
 */
@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // ── Busca por chave única (upsert) ────────────────────────────────────────

    Optional<Lote> findByFonteAndIdExterno(String fonte, String idExterno);

    // ── Upsert nativo PostgreSQL ───────────────────────────────────────────────
    /**
     * Insere ou atualiza um lote usando ON CONFLICT DO UPDATE.
     * Equivalente ao salvar_ou_atualizar() do Python.
     */
    @Modifying
    @Query(value = """
        INSERT INTO lotes (
            fonte, id_externo, url_original, tipo, origem, status,
            valor_avaliacao, valor_lance_inicial, valor_incremento,
            aceita_financiamento, aceita_fgts, banco_financiador,
            logradouro, bairro, cidade, estado, cep,
            descricao, area_m2, data_leilao,
            data_primeiro_leilao, data_segundo_leilao,
            coletado_em, html_raw
        ) VALUES (
            :#{#l.fonte}, :#{#l.idExterno}, :#{#l.urlOriginal},
            CAST(:#{#l.tipo.name()} AS tipolote),
            CAST(:#{#l.origem?.name()} AS origemleilao),
            CAST(:#{#l.status.name()} AS statuslote),
            :#{#l.valorAvaliacao}, :#{#l.valorLanceInicial}, :#{#l.valorIncremento},
            :#{#l.aceitaFinanciamento}, :#{#l.aceitaFgts}, :#{#l.bancoFinanciador},
            :#{#l.logradouro}, :#{#l.bairro}, :#{#l.cidade}, :#{#l.estado}, :#{#l.cep},
            :#{#l.descricao}, :#{#l.areaM2}, :#{#l.dataLeilao},
            :#{#l.dataPrimeiroLeilao}, :#{#l.dataSegundoLeilao},
            NOW(), :#{#l.htmlRaw}
        )
        ON CONFLICT (fonte, id_externo) DO UPDATE SET
            valor_lance_inicial = EXCLUDED.valor_lance_inicial,
            status              = EXCLUDED.status,
            data_leilao         = EXCLUDED.data_leilao,
            atualizado_em       = NOW()
        """, nativeQuery = true)
    void upsert(@Param("l") Lote lote);

    // ── Filtros para listagem (base da Fase 3) ────────────────────────────────

    @Query("""
        SELECT l FROM Lote l
        WHERE l.status = :status
          AND (:cidade   IS NULL OR LOWER(l.cidade) = LOWER(:cidade))
          AND (:estado   IS NULL OR l.estado = :estado)
          AND (:tipo     IS NULL OR l.tipo   = :tipo)
          AND (:aceitaFin IS NULL OR l.aceitaFinanciamento = :aceitaFin)
          AND (:valorMax IS NULL OR l.valorLanceInicial <= :valorMax)
          AND l.dataLeilao >= CURRENT_TIMESTAMP
        ORDER BY l.dataLeilao ASC
        LIMIT :limite
        """)
    List<Lote> buscarPorFiltros(
            @Param("status")    StatusLote status,
            @Param("cidade")    String cidade,
            @Param("estado")    String estado,
            @Param("tipo")      TipoLote tipo,
            @Param("aceitaFin") Boolean aceitaFinanciamento,
            @Param("valorMax")  BigDecimal valorMaximo,
            @Param("limite")    int limite
    );

    // ── Queries para o motor de score ─────────────────────────────────────────

    /** Lotes sem score calculado (processamento em batch). */
    List<Lote> findByScoreOportunidadeIsNull();

    /** Lotes coletados hoje (para recalcular após cada coleta). */
    @Query("SELECT l FROM Lote l WHERE CAST(l.coletadoEm AS date) = CURRENT_DATE")
    List<Lote> findColetadosHoje();

    // ── Monitoramento ─────────────────────────────────────────────────────────

    /** Contagem por fonte — útil para logs do scheduler. */
    @Query("SELECT l.fonte, COUNT(l) FROM Lote l GROUP BY l.fonte")
    List<Object[]> contarPorFonte();

    /** Quantos lotes foram coletados hoje. */
    @Query("SELECT COUNT(l) FROM Lote l WHERE CAST(l.coletadoEm AS date) = CURRENT_DATE")
    long contarColetadosHoje();

    /** Top N lotes por score — para a listagem principal. */
    @Query("""
        SELECT l FROM Lote l
        WHERE l.status = 'DISPONIVEL'
          AND l.scoreOportunidade IS NOT NULL
          AND l.scoreOportunidade >= :scoreMinimo
        ORDER BY l.scoreOportunidade DESC
        LIMIT :limite
        """)
    List<Lote> findTopPorScore(
            @Param("scoreMinimo") BigDecimal scoreMinimo,
            @Param("limite")      int limite
    );
}
