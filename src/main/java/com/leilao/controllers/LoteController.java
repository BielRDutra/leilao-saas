package com.leilao.controllers;

import com.leilao.model.Lote;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;
import com.leilao.repository.LoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller RESTful para exposição dos Lotes ao frontend (Fase 3).
 */
@RestController
@RequestMapping("/api/v1/lotes")
@CrossOrigin(origins = "*") // Permite chamadas do frontend (ex: Next.js) rodando em outra porta
public class LoteController {

    private final LoteRepository loteRepository;

    // Injeção de dependência via construtor (melhor prática em relação ao @Autowired)
    public LoteController(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    @GetMapping("/top")
    public ResponseEntity<List<Lote>> listarTopOportunidades(
            @RequestParam(defaultValue = "50.0") BigDecimal scoreMinimo,
            @RequestParam(defaultValue = "20") int limite
    ) {
        List<Lote> lotes = loteRepository.findTopPorScore(scoreMinimo, limite);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping
    public ResponseEntity<List<Lote>> buscarLotes(
            @RequestParam(defaultValue = "DISPONIVEL") StatusLote status,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) TipoLote tipo,
            @RequestParam(required = false) Boolean aceitaFinanciamento,
            @RequestParam(required = false) BigDecimal valorMaximo,
            @RequestParam(defaultValue = "50") int limite
    ) {
        List<Lote> lotes = loteRepository.buscarPorFiltros(
                status, cidade, estado, tipo, aceitaFinanciamento, valorMaximo, limite
        );
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lote> buscarPorId(@PathVariable Long id) {
        return loteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
