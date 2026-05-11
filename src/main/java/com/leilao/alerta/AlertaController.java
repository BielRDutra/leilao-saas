package com.leilao.alerta;

import com.leilao.alerta.dto.AssinanteRespostaDTO;
import com.leilao.alerta.dto.CriarAssinanteDTO;
import com.leilao.alerta.service.AlertaFacadeService;
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
 * Fix #2:  controller não acessa mais repositórios diretamente.
 *          Toda lógica delegada para AlertaFacadeService.
 * Fix #17: EntityNotFoundException lançada no service, não aqui.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaFacadeService facadeService;

    @PostMapping("/assinantes")
    public ResponseEntity<AssinanteRespostaDTO> cadastrar(
            @Valid @RequestBody CriarAssinanteDTO dto) {
        log.info("[alertas] POST /assinantes");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(facadeService.cadastrarAssinante(dto));
    }

    @GetMapping("/assinantes")
    public ResponseEntity<List<AssinanteRespostaDTO>> listar() {
        return ResponseEntity.ok(facadeService.listarAssinantes());
    }

    @DeleteMapping("/assinantes/{id}")
    public ResponseEntity<Map<String, String>> cancelar(@PathVariable Long id) {
        log.info("[alertas] DELETE /assinantes/{}", id);
        facadeService.cancelarAssinante(id);
        return ResponseEntity.ok(Map.of("mensagem", "Assinante cancelado com sucesso."));
    }

    @PostMapping("/processar")
    public ResponseEntity<Map<String, Integer>> processarAlertas() {
        log.info("[alertas] POST /processar");
        int enviados = facadeService.processarAlertasDiarios();
        return ResponseEntity.ok(Map.of("notificacoesEnviadas", enviados));
    }

    @PostMapping("/processar/lote/{id}")
    public ResponseEntity<Map<String, Integer>> processarLote(@PathVariable Long id) {
        log.info("[alertas] POST /processar/lote/{}", id);
        int enviados = facadeService.processarLote(id);
        return ResponseEntity.ok(Map.of("notificacoesEnviadas", enviados));
    }
}
