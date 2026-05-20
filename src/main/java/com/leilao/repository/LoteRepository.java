package com.leilao.repository;

import com.leilao.model.Lote;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para a entidade Lote.
 *
 * Fix #4: substituídas queries JPQL com LIMIT (não-padrão) por Pageable,
 * que é a forma portável e recomendada pelo Spring Data.
 * Fix #9: removido import LocalDateTime não utilizado.
 */
@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // ── Upsert / busca por chave ──────────────────────────────────────────────

    Optional<Lote> findByFonteAndIdExterno(String fonte, String idExterno);

    // ── Filtros para listagem ─────────────────────────────────────────────────

    /**
     * Fix #4 e #13: a query agora recebe Pageable para paginação real no banco.
     * O filtro de scoreMinimo foi movido para dentro da query (elimina filtragem
     * em memória que gerava resultados inconsistentes com o limite aplicado).
     */
    @Query("""
        SELECT l FROM Lote l
        WHERE l.status = :status
          AND (:cidade    IS NULL OR LOWER(l.cidade) = LOWER(:cidade))
          AND (:estado    IS NULL OR l.estado = :estado)
          AND (:tipo      IS NULL OR l.tipo   = :tipo)
          AND (:aceitaFin IS NULL OR l.aceitaFinanciamento = :aceitaFin)
          AND (:valorMax  IS NULL OR l.valorLanceInicial <= :valorMax)
          AND (:scoreMin  IS NULL OR l.scoreOportunidade >= :scoreMin)
          AND l.dataLeilao >= CURRENT_TIMESTAMP
        ORDER BY l.scoreOportunidade DESC NULLS LAST, l.dataLeilao ASC
        """)
    List<Lote> buscarPorFiltros(
            @Param("status")    StatusLote status,
            @Param("cidade")    String cidade,
            @Param("estado")    String estado,
            @Param("tipo")      TipoLote tipo,
            @Param("aceitaFin") Boolean aceitaFinanciamento,
            @Param("valorMax")  BigDecimal valorMaximo,
            @Param("scoreMin")  BigDecimal scoreMinimo,
            Pageable pageable
    );

    // ── Score ─────────────────────────────────────────────────────────────────

    List<Lote> findByScoreOportunidadeIsNull();

    @Query("SELECT l FROM Lote l WHERE CAST(l.coletadoEm AS date) = CURRENT_DATE")
    List<Lote> findColetadosHoje();

    /**
     * Fix #4: LIMIT removido — usa Pageable para limitar o resultado
     * de forma portável e segura.
     */
    @Query("""
        SELECT l FROM Lote l
        WHERE l.status = 'DISPONIVEL'
          AND l.scoreOportunidade IS NOT NULL
          AND l.scoreOportunidade >= :scoreMinimo
        ORDER BY l.scoreOportunidade DESC
        """)
    List<Lote> findTopPorScore(
            @Param("scoreMinimo") BigDecimal scoreMinimo,
            Pageable pageable
    );

    // ── Monitoramento ─────────────────────────────────────────────────────────

    @Query("SELECT l.fonte, COUNT(l) FROM Lote l GROUP BY l.fonte")
    List<Object[]> contarPorFonte();

    @Query("SELECT COUNT(l) FROM Lote l WHERE CAST(l.coletadoEm AS date) = CURRENT_DATE")
    long contarColetadosHoje();

    @Query("SELECT l.tipo, COUNT(l) FROM Lote l GROUP BY l.tipo")
    List<Object[]> contarPorTipo();

    @Query("SELECT AVG(l.scoreOportunidade) FROM Lote l WHERE l.scoreOportunidade IS NOT NULL")
    BigDecimal scoreMediano();

    long countByStatus(StatusLote status);

    long countByScoreOportunidadeIsNotNull();

    Optional<Lote> findTopByOrderByColetadoEmDesc();

    /**
     * Fix #3: query dedicada para maior desconto — evita findAll() em memória.
     * Calcula o maior desconto diretamente no banco.
     */
    @Query("""
        SELECT l FROM Lote l
        WHERE l.valorAvaliacao IS NOT NULL
          AND l.valorAvaliacao > 0
          AND l.status = 'DISPONIVEL'
        ORDER BY (1 - l.valorLanceInicial / l.valorAvaliacao) DESC
        """)
    List<Lote> findLoteComMaiorDesconto(Pageable pageable);
}
