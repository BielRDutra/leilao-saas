package com.leilao.alerta;

import com.leilao.alerta.dto.AssinanteRespostaDTO;
import com.leilao.alerta.dto.CriarAssinanteDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de alertas.
 *
 * POST   /api/v1/alertas/assinantes            → cadastra assinante
 * GET    /api/v1/alertas/assinantes            → lista assinantes
 * DELETE /api/v1/alertas/assinantes/{id}       → cancela assinante
 * POST   /api/v1/alertas/processar             → dispara alertas manualmente
 * POST   /api/v1/alertas/processar/lote/{id}   → alerta para um lote específico
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AssinanteRepository assinanteRepo;
    private final AlertaService        alertaService;
    private final com.leilao.repository.LoteRepository loteRepo;

    // ── Assinantes ────────────────────────────────────────────────────────────

    /**
     * Cadastra um novo assinante de alertas.
     *
     * Body exemplo:
     * {
     *   "nome": "João Silva",
     *   "email": "joao@gmail.com",
     *   "whatsapp": "5511999990000",
     *   "scoreMinimo": 65.0,
     *   "cidade": "São Paulo",
     *   "estado": "SP",
     *   "tipoLote": "IMOVEL_RESIDENCIAL"
     * }
     */
    @PostMapping("/assinantes")
    public ResponseEntity<AssinanteRespostaDTO> cadastrar(
            @Valid @RequestBody CriarAssinanteDTO dto) {

        Assinante assinante = assinanteRepo.save(dto.toEntity());
        log.info("[alertas] Novo assinante: {} | score≥{} | {}/{}",
            assinante.getNome(), assinante.getScoreMinimo(),
            assinante.getCidade(), assinante.getEstado());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AssinanteRespostaDTO.from(assinante));
    }

    /** Lista todos os assinantes ativos (dados mascarados). */
    @GetMapping("/assinantes")
    public ResponseEntity<List<AssinanteRespostaDTO>> listar() {
        List<AssinanteRespostaDTO> lista = assinanteRepo.findByAtivoTrue()
            .stream()
            .map(AssinanteRespostaDTO::from)
            .toList();
        return ResponseEntity.ok(lista);
    }

    /** Cancela (desativa) um assinante. */
    @DeleteMapping("/assinantes/{id}")
    public ResponseEntity<Map<String, String>> cancelar(@PathVariable Long id) {
        Assinante assinante = assinanteRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Assinante não encontrado: " + id));

        assinante.setAtivo(false);
        assinanteRepo.save(assinante);

        log.info("[alertas] Assinante {} cancelado.", id);
        return ResponseEntity.ok(Map.of("mensagem", "Assinante cancelado com sucesso."));
    }

    // ── Processamento de alertas ──────────────────────────────────────────────

    /**
     * Dispara o processamento de alertas para os lotes coletados hoje.
     * Normalmente chamado automaticamente após o scraping.
     * Pode ser chamado manualmente para testes.
     */
    @PostMapping("/processar")
    public ResponseEntity<Map<String, Integer>> processarAlertas() {
        log.info("[alertas] Processamento manual solicitado via API.");
        int enviados = alertaService.processarAlertasDiarios();
        return ResponseEntity.ok(Map.of("notificacoesEnviadas", enviados));
    }

    /**
     * Dispara alertas para um lote específico.
     * Útil para testar o fluxo completo com um lote real.
     */
    @PostMapping("/processar/lote/{id}")
    public ResponseEntity<Map<String, Integer>> processarLote(@PathVariable Long id) {
        com.leilao.model.Lote lote = loteRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado: " + id));

        int enviados = alertaService.processarLote(lote);
        return ResponseEntity.ok(Map.of("notificacoesEnviadas", enviados));
    }
}
