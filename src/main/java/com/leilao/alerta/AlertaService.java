package com.leilao.alerta;

import com.leilao.model.Lote;
import com.leilao.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestra o fluxo de alertas:
 *   1. Busca lotes coletados hoje com score calculado
 *   2. Filtra assinantes cujos critérios são satisfeitos
 *   3. Envia e-mail e/ou WhatsApp (idempotente — evita re-envios)
 *   4. Registra o histórico
 *
 * Fix #11: propagação de transações corrigida.
 *   processarAlertasDiarios é NOT_SUPPORTED (sem transação longa aberta durante I/O de rede).
 *   processarLote é REQUIRES_NEW (transação curta e isolada por lote).
 *   registrarHistorico é REQUIRES_NEW (persiste mesmo se a transação pai falhar).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AssinanteRepository       assinanteRepo;
    private final HistoricoAlertaRepository historicoRepo;
    private final LoteRepository            loteRepo;
    private final EmailService              emailService;
    private final WhatsAppService           whatsAppService;

    /**
     * Fix #11: NOT_SUPPORTED — não mantém transação aberta durante chamadas de rede
     * (envio de e-mail / HTTP para Z-API). Cada lote abre sua própria transação.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int processarAlertasDiarios() {
        List<Lote> lotes = loteRepo.findColetadosHoje();
        log.info("[alertas] Verificando {} lotes coletados hoje...", lotes.size());

        int total = 0;
        for (Lote lote : lotes) {
            total += processarLote(lote);
        }
        log.info("[alertas] {} notificações enviadas.", total);
        return total;
    }

    /**
     * Fix #11: REQUIRES_NEW — transação independente por lote.
     * Isola falhas: erro em um lote não afeta os demais.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processarLote(Lote lote) {
        if (lote.getScoreOportunidade() == null) return 0;

        List<Assinante> candidatos = assinanteRepo.buscarAssinantesParaLote(
            lote.getEstado(),
            lote.getCidade(),
            lote.getTipo()
        );

        int enviados = 0;
        for (Assinante assinante : candidatos) {
            if (lote.getScoreOportunidade().compareTo(assinante.getScoreMinimo()) < 0) continue;
            enviados += despacharNotificacoes(assinante, lote);
        }
        return enviados;
    }

    // ── Despacho ──────────────────────────────────────────────────────────────

    private int despacharNotificacoes(Assinante assinante, Lote lote) {
        int enviados = 0;
        if (assinante.temEmail())    enviados += enviarCanal(assinante, lote, CanalAlerta.EMAIL);
        if (assinante.temWhatsApp()) enviados += enviarCanal(assinante, lote, CanalAlerta.WHATSAPP);
        return enviados;
    }

    private int enviarCanal(Assinante assinante, Lote lote, CanalAlerta canal) {
        if (historicoRepo.existsByAssinanteIdAndLoteIdAndCanal(
                assinante.getId(), lote.getId(), canal)) {
            log.debug("[alertas] Já enviado: assinante={} lote={} canal={}",
                assinante.getId(), lote.getId(), canal);
            return 0;
        }

        boolean sucesso = false;
        String  erroMsg = null;
        try {
            sucesso = switch (canal) {
                case EMAIL    -> emailService.enviarAlerta(assinante, lote);
                case WHATSAPP -> whatsAppService.enviarAlerta(assinante, lote);
            };
        } catch (Exception e) {
            erroMsg = e.getMessage();
            log.error("[alertas] Erro ao enviar {} para assinante {}: {}",
                canal, assinante.getId(), e.getMessage());
        }

        registrarHistorico(assinante, lote, canal, sucesso, erroMsg);
        return sucesso ? 1 : 0;
    }

    /**
     * Fix #11: REQUIRES_NEW — persiste o histórico em transação própria,
     * garantindo que o registro seja salvo mesmo se a transação do lote falhar.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarHistorico(
            Assinante assinante, Lote lote,
            CanalAlerta canal, boolean sucesso, String erroMensagem) {
        try {
            historicoRepo.save(HistoricoAlerta.builder()
                .assinante(assinante)
                .lote(lote)
                .canal(canal)
                .sucesso(sucesso)
                .erroMensagem(erroMensagem)
                .build());
        } catch (Exception e) {
            log.debug("[alertas] Histórico já existe (race condition ignorada): {}",
                e.getMessage());
        }
    }
}
