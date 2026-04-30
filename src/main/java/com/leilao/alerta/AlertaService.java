package com.leilao.alerta;

import com.leilao.model.Lote;
import com.leilao.repository.LoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestra todo o fluxo de alertas:
 *   1. Busca lotes recém pontuados (coletados hoje)
 *   2. Para cada lote, encontra assinantes cujos critérios são satisfeitos
 *   3. Para cada assinante, envia e-mail e/ou WhatsApp (se ainda não enviado)
 *   4. Registra o histórico para evitar re-envios
 *
 * Chamado pelo ScraperScheduler após o cálculo de score.
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
     * Processa alertas para os lotes coletados hoje.
     * Retorna o total de notificações enviadas.
     */
    @Transactional
    public int processarAlertasDiarios() {
        List<Lote> lotes = loteRepo.findColetadosHoje();
        log.info("[alertas] Verificando {} lotes coletados hoje...", lotes.size());

        int totalEnviados = 0;
        for (Lote lote : lotes) {
            totalEnviados += processarLote(lote);
        }

        log.info("[alertas] {} notificações enviadas no total.", totalEnviados);
        return totalEnviados;
    }

    /**
     * Processa alertas para um único lote.
     * Usado após cálculo de score individual via API.
     */
    @Transactional
    public int processarLote(Lote lote) {
        // Lote sem score ou abaixo do mínimo possível — ignora
        if (lote.getScoreOportunidade() == null) return 0;

        // Busca assinantes cujos filtros de localização/tipo batem com o lote
        List<Assinante> candidatos = assinanteRepo.buscarAssinantesParaLote(
            lote.getEstado(),
            lote.getCidade(),
            lote.getTipo()
        );

        int enviados = 0;
        for (Assinante assinante : candidatos) {
            // Verifica se o score do lote atinge o threshold do assinante
            if (lote.getScoreOportunidade().compareTo(assinante.getScoreMinimo()) < 0) {
                continue;
            }
            enviados += despacharNotificacoes(assinante, lote);
        }

        return enviados;
    }

    // ── Despacho por canal ────────────────────────────────────────────────────

    private int despacharNotificacoes(Assinante assinante, Lote lote) {
        int enviados = 0;

        if (assinante.temEmail()) {
            enviados += enviarCanal(assinante, lote, CanalAlerta.EMAIL);
        }
        if (assinante.temWhatsApp()) {
            enviados += enviarCanal(assinante, lote, CanalAlerta.WHATSAPP);
        }

        return enviados;
    }

    private int enviarCanal(Assinante assinante, Lote lote, CanalAlerta canal) {
        // Checa se já foi enviado (idempotência)
        if (historicoRepo.existsByAssinanteIdAndLoteIdAndCanal(
                assinante.getId(), lote.getId(), canal)) {
            log.debug("[alertas] Já enviado: assinante={} lote={} canal={}",
                assinante.getId(), lote.getId(), canal);
            return 0;
        }

        boolean sucesso;
        String erroMsg = null;

        try {
            sucesso = switch (canal) {
                case EMAIL     -> emailService.enviarAlerta(assinante, lote);
                case WHATSAPP  -> whatsAppService.enviarAlerta(assinante, lote);
            };
        } catch (Exception e) {
            sucesso = false;
            erroMsg = e.getMessage();
            log.error("[alertas] Erro ao enviar {} para assinante {}: {}",
                canal, assinante.getId(), e.getMessage());
        }

        // Registra no histórico independentemente do resultado
        registrarHistorico(assinante, lote, canal, sucesso, erroMsg);

        return sucesso ? 1 : 0;
    }

    private void registrarHistorico(
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
            // Violação de unique constraint = já foi enviado por outra thread
            log.debug("[alertas] Histórico já existe (race condition ignorada): {}", e.getMessage());
        }
    }
}
