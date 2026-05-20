package com.leilao.alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssinanteRepository extends JpaRepository<Assinante, Long> {

    /** Busca assinantes ativos cujos critérios podem ser satisfeitos por um lote. */
    @Query("""
        SELECT a FROM Assinante a
        WHERE a.ativo = true
          AND (:estado   IS NULL OR a.estado   IS NULL OR a.estado   = :estado)
          AND (:cidade   IS NULL OR a.cidade   IS NULL
               OR LOWER(a.cidade) = LOWER(:cidade))
          AND (:tipoLote IS NULL OR a.tipoLote IS NULL
               OR a.tipoLote = :tipoLote)
        """)
    List<Assinante> buscarAssinantesParaLote(
        @Param("estado")   String estado,
        @Param("cidade")   String cidade,
        @Param("tipoLote") com.leilao.model.TipoLote tipoLote
    );

    List<Assinante> findByAtivoTrue();
}
