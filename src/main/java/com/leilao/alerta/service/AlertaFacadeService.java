package com.leilao.alerta.service;

import com.leilao.alerta.AlertaService;
import com.leilao.alerta.Assinante;
import com.leilao.alerta.AssinanteRepository;
import com.leilao.alerta.dto.AssinanteRespostaDTO;
import com.leilao.alerta.dto.CriarAssinanteDTO;
import com.leilao.model.Lote;
import com.leilao.repository.LoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Camada de service para operações de alertas expostas via API REST.
 *
 * Fix #2:  concentra lógica que estava no controller (acesso a repositórios).
 * Fix #17: EntityNotFoundException lançada aqui, capturada pelo GlobalExceptionHandler.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaFacadeService {

    private final AssinanteRepository assinanteRepo;
    private final AlertaService        alertaService;
    private final LoteRepository       loteRepo;

    @Transactional
    public AssinanteRespostaDTO cadastrarAssinante(CriarAssinanteDTO dto) {
        Assinante assinante = assinanteRepo.save(dto.toEntity());
        log.info("[alertas] Novo assinante: {} | score≥{} | {}/{}",
            assinante.getNome(), assinante.getScoreMinimo(),
            assinante.getCidade(), assinante.getEstado());
        return AssinanteRespostaDTO.from(assinante);
    }

    public List<AssinanteRespostaDTO> listarAssinantes() {
        return assinanteRepo.findByAtivoTrue()
            .stream()
            .map(AssinanteRespostaDTO::from)
            .toList();
    }

    @Transactional
    public void cancelarAssinante(Long id) {
        Assinante assinante = assinanteRepo.findById(id) // Fix #17
            .orElseThrow(() -> new EntityNotFoundException("Assinante não encontrado: " + id));
        assinante.setAtivo(false);
        assinanteRepo.save(assinante);
        log.info("[alertas] Assinante {} cancelado.", id);
    }

    public int processarAlertasDiarios() {
        return alertaService.processarAlertasDiarios();
    }

    public int processarLote(Long loteId) {
        Lote lote = loteRepo.findById(loteId) // Fix #17
            .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado: " + loteId));
        return alertaService.processarLote(lote);
    }
}
