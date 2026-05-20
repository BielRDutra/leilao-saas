package com.leilao.controller;

import com.leilao.dto.*;
import com.leilao.service.LoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API REST de lotes de leilão.
 *
 * Base URL: /api/v1
 *
 * Endpoints:
 *   GET  /lotes/ranking          → top lotes por score
 *   GET  /lotes/buscar           → busca com filtros
 *   GET  /lotes/{id}             → detalhe de um lote
 *   GET  /lotes/resumo           → estatísticas da plataforma
 *   POST /score/recalcular       → força recálculo de todos os scores
 *   POST /score/lote/{id}        → calcula score de um lote específico
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    // ── GET /lotes/ranking ────────────────────────────────────────────────────

    /**
     * Retorna os lotes com maior score de oportunidade.
     *
     * Parâmetros de query:
     *   limite  → quantos lotes retornar (default: 20, max: 100)
     *   page    → página (paginação Spring, default: 0)
     *   size    → itens por página (default: 20)
     *
     * Exemplo: GET /api/v1/lotes/ranking?limite=50&page=0&size=10
     */
    @GetMapping("/lotes/ranking")
    public ResponseEntity<Page<RankingItemDTO>> ranking(
            @RequestParam(defaultValue = "100") int limite,
            @PageableDefault(size = 20, sort = "scoreOportunidade") Pageable pageable
    ) {
        log.info("[api] GET /lotes/ranking — limite={}", limite);
        return ResponseEntity.ok(loteService.ranking(limite, pageable));
    }

    // ── GET /lotes/buscar ─────────────────────────────────────────────────────

    /**
     * Busca lotes com filtros opcionais.
     *
     * Parâmetros de query:
     *   cidade              → ex: "São Paulo"
     *   estado              → UF, ex: "SP"
     *   tipo                → IMOVEL_RESIDENCIAL | IMOVEL_COMERCIAL | VEICULO...
     *   aceitaFinanciamento → true | false
     *   aceitaFgts          → true | false
     *   valorMaximo         → valor máximo do lance
     *   scoreMinimo         → score mínimo de oportunidade
     *   limite              → máximo de resultados (default: 50)
     *
     * Exemplo: GET /api/v1/lotes/buscar?cidade=Curitiba&estado=PR&aceitaFinanciamento=true
     */
    @GetMapping("/lotes/buscar")
    public ResponseEntity<Page<LoteDTO>> buscar(
            @Valid @ModelAttribute FiltroLoteDTO filtro,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("[api] GET /lotes/buscar — filtro={}", filtro);
        return ResponseEntity.ok(loteService.buscar(filtro, pageable));
    }

    // ── GET /lotes/{id} ───────────────────────────────────────────────────────

    /**
     * Retorna o detalhe completo de um lote pelo ID.
     * Inclui todos os sub-scores e classificação.
     *
     * Exemplo: GET /api/v1/lotes/42
     */
    @GetMapping("/lotes/{id}")
    public ResponseEntity<LoteDTO> detalhe(@PathVariable Long id) {
        log.info("[api] GET /lotes/{}", id);
        return ResponseEntity.ok(loteService.buscarPorId(id));
    }

    // ── GET /lotes/resumo ─────────────────────────────────────────────────────

    /**
     * Retorna estatísticas gerais: total de lotes, por fonte, score mediano, etc.
     * Útil para o dashboard da aplicação.
     *
     * Exemplo: GET /api/v1/lotes/resumo
     */
    @GetMapping("/lotes/resumo")
    public ResponseEntity<ResumoDTO> resumo() {
        log.info("[api] GET /lotes/resumo");
        return ResponseEntity.ok(loteService.resumo());
    }

    // ── POST /score/recalcular ────────────────────────────────────────────────

    /**
     * Força o recálculo de score de todos os lotes no banco.
     * Útil quando os pesos do score forem alterados em application.properties.
     *
     * Retorna: { "lotesPontuados": N }
     *
     * Exemplo: POST /api/v1/score/recalcular
     */
    @PostMapping("/score/recalcular")
    public ResponseEntity<Map<String, Integer>> recalcularScore() {
        log.info("[api] POST /score/recalcular");
        int pontuados = loteService.recalcularScore();
        return ResponseEntity.ok(Map.of("lotesPontuados", pontuados));
    }

    // ── POST /score/lote/{id} ─────────────────────────────────────────────────

    /**
     * Calcula ou recalcula o score de um lote específico.
     * Retorna o lote atualizado com o novo score.
     *
     * Exemplo: POST /api/v1/score/lote/42
     */
    @PostMapping("/score/lote/{id}")
    public ResponseEntity<LoteDTO> calcularScoreLote(@PathVariable Long id) {
        log.info("[api] POST /score/lote/{}", id);
        return ResponseEntity.ok(loteService.calcularScoreLote(id));
    }
}
