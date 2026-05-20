package com.leilao.alerta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoAlertaRepository extends JpaRepository<HistoricoAlerta, Long> {

    /** Verifica se o alerta já foi enviado para este assinante/lote/canal. */
    boolean existsByAssinanteIdAndLoteIdAndCanal(
        Long assinanteId,
        Long loteId,
        CanalAlerta canal
    );
}
