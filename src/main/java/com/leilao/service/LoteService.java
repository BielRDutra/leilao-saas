package com.leilao.service;

import com.leilao.config.ScoreConfig;
import com.leilao.dto.*;
import com.leilao.model.Lote;
import com.leilao.model.StatusLote;
import com.leilao.repository.LoteRepository;
import com.leilao.score.MotorScore;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Camada de negócio da API REST.
 *
 * Fix #3:  maior desconto calculado no banco, não em memória.
 * Fix #4:  findTopPorScore e buscarPorFiltros usam Pageable.
 * Fix #13: scoreMinimo passado para a query do banco, elimina filtragem em memória.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;
    private final MotorScore     motorScore;
    private final ScoreConfig    scoreConfig;

    // ── Ranking ───────────────────────────────────────────────────────────────

    public Page<RankingItemDTO> ranking(int limite, Pageable pageable) {
        BigDecimal scoreMin = BigDecimal.valueOf(scoreConfig.getMinimoListagem());

        // Fix #4: passa PageRequest com limite como size — sem LIMIT na JPQL
        Pageable limitado = PageRequest.of(0, limite,
            org.springframework.data.domain.Sort.by("scoreOportunidade").descending());

        List<Lote> lotes = loteRepository.findTopPorScore(scoreMin, limitado);
        List<RankingItemDTO> items = lotes.stream().map(RankingItemDTO::from).toList();

        int start  = (int) pageable.getOffset();
        int end    = Math.min(start + pageable.getPageSize(), items.size());
        List<RankingItemDTO> pagina = start < items.size()
            ? items.subList(start, end) : List.of();

        return new PageImpl<>(pagina, pageable, items.size());
    }

    // ── Busca filtrada ────────────────────────────────────────────────────────

    /**
     * Fix #13: scoreMinimo agora é passado para a query do banco.
     * Antes, o scoreMinimo era filtrado em memória APÓS aplicar o limite,
     * o que produzia resultados com menos itens que o esperado.
     */
    public Page<LoteDTO> buscar(FiltroLoteDTO filtro, Pageable pageable) {
        Pageable limitado = PageRequest.of(
            pageable.getPageNumber(),
            filtro.limiteEfetivo(),
            pageable.getSortOr(org.springframework.data.domain.Sort.by("dataLeilao").ascending())
        );

        List<Lote> lotes = loteRepository.buscarPorFiltros(
            StatusLote.DISPONIVEL,
            filtro.cidade(),
            filtro.estado() != null ? filtro.estado().toUpperCase() : null,
            filtro.tipo(),
            filtro.aceitaFinanciamento(),
            filtro.valorMaximo(),
            filtro.scoreMinimo(), // Fix #13: direto no banco
            limitado
        );

        List<LoteDTO> dtos  = lotes.stream().map(LoteDTO::from).toList();
        return new PageImpl<>(dtos, pageable, dtos.size());
    }

    // ── Detalhe ───────────────────────────────────────────────────────────────

    public LoteDTO buscarPorId(Long id) {
        return loteRepository.findById(id)
            .map(LoteDTO::from)
            .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado: " + id));
    }

    // ── Resumo / estatísticas ─────────────────────────────────────────────────

    public ResumoDTO resumo() {
        long total       = loteRepository.count();
        long disponiveis = loteRepository.countByStatus(StatusLote.DISPONIVEL);
        long comScore    = loteRepository.countByScoreOportunidadeIsNotNull();
        long hoje        = loteRepository.contarColetadosHoje();

        Map<String, Long> porFonte = loteRepository.contarPorFonte()
            .stream().collect(Collectors.toMap(
                r -> (String) r[0],
                r -> (Long)   r[1]
            ));

        Map<String, Long> porTipo = loteRepository.contarPorTipo()
            .stream().collect(Collectors.toMap(
                r -> r[0].toString(),
                r -> (Long) r[1]
            ));

        BigDecimal scoreMediano = loteRepository.scoreMediano();

        // Fix #3: maior desconto calculado no banco — sem findAll() em memória
        BigDecimal maiorDesconto = loteRepository
            .findLoteComMaiorDesconto(PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(Lote::getDescontoPercentual)
            .orElse(null);

        LocalDateTime ultimaColeta = loteRepository
            .findTopByOrderByColetadoEmDesc()
            .map(Lote::getColetadoEm)
            .orElse(null);

        return new ResumoDTO(
            total, disponiveis, comScore, hoje,
            porFonte, porTipo, scoreMediano, maiorDesconto, ultimaColeta
        );
    }

    // ── Score ─────────────────────────────────────────────────────────────────

    @Transactional
    public int recalcularScore() {
        log.info("[api] Recálculo de score solicitado via API.");
        return motorScore.recalcularTodos();
    }

    @Transactional
    public LoteDTO calcularScoreLote(Long id) {
        Lote lote = loteRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado: " + id));
        motorScore.calcularESalvar(lote);
        return LoteDTO.from(lote);
    }
}
