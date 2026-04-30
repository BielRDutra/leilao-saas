package com.leilao.service;

import com.leilao.config.ScoreConfig;
import com.leilao.dto.*;
import com.leilao.model.Lote;
import com.leilao.model.StatusLote;
import com.leilao.repository.LoteRepository;
import com.leilao.score.MotorScore;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Camada de negócio da API REST.
 * Isola o controller das queries JPA e do motor de score.
 */
@SuppressWarnings("all")
@Service
public class LoteService {

    private static final Logger log = LoggerFactory.getLogger(LoteService.class);

    private final LoteRepository loteRepository;
    private final MotorScore motorScore;
    private final ScoreConfig scoreConfig;

    public LoteService(
        LoteRepository loteRepository,
        MotorScore motorScore,
        ScoreConfig scoreConfig
    ) {
        this.loteRepository = loteRepository;
        this.motorScore = motorScore;
        this.scoreConfig = scoreConfig;
    }

    // ── Ranking ───────────────────────────────────────────────────────────────

    /**
     * Retorna os N lotes com maior score de oportunidade.
     * Equivale a "melhores oportunidades agora".
     */
    public Page<RankingItemDTO> ranking(int limite, Pageable pageable) {
        BigDecimal scoreMin = BigDecimal.valueOf(scoreConfig.getMinimoListagem());
        List<Lote> lotes = loteRepository.findTopPorScore(scoreMin, limite);

        List<RankingItemDTO> items = lotes.stream()
            .map(RankingItemDTO::from)
            .toList();

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), items.size());
        List<RankingItemDTO> pagina = (start < items.size())
            ? items.subList(start, end)
            : List.of();

        return new PageImpl<>(pagina, pageable, items.size());
    }

    // ── Busca filtrada ────────────────────────────────────────────────────────

    /**
     * Busca lotes aplicando filtros opcionais.
     * Todos os parâmetros do FiltroLoteDTO são opcionais.
     */
    public Page<LoteDTO> buscar(FiltroLoteDTO filtro, Pageable pageable) {
        List<Lote> lotes = loteRepository.buscarPorFiltros(
            StatusLote.DISPONIVEL,
            filtro.cidade(),
            filtro.estado() != null ? filtro.estado().toUpperCase() : null,
            filtro.tipo(),
            filtro.aceitaFinanciamento(),
            filtro.valorMaximo(),
            filtro.limiteEfetivo()
        );

        // Filtro de score mínimo (em memória — campo calculado)
        if (filtro.scoreMinimo() != null) {
            lotes = lotes.stream()
                .filter(l -> l.getScoreOportunidade() != null
                    && l.getScoreOportunidade().compareTo(filtro.scoreMinimo()) >= 0)
                .toList();
        }

        List<LoteDTO> dtos = lotes.stream().map(LoteDTO::from).toList();

        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), dtos.size());
        List<LoteDTO> pagina = (start < dtos.size()) ? dtos.subList(start, end) : List.of();

        return new PageImpl<>(pagina, pageable, dtos.size());
    }

    // ── Detalhe ───────────────────────────────────────────────────────────────

    /** Retorna um lote pelo ID com todos os campos, incluindo score detalhado. */
    public LoteDTO buscarPorId(Long id) {
        Lote lote = loteRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado: " + id));
        return LoteDTO.from(lote);
    }

    // ── Resumo / estatísticas ─────────────────────────────────────────────────

    /** Retorna estatísticas gerais da plataforma. */
    public ResumoDTO resumo() {
        long total       = loteRepository.count();
        long disponiveis = loteRepository.countByStatus(StatusLote.DISPONIVEL);
        long comScore    = loteRepository.countByScoreOportunidadeIsNotNull();
        long hoje        = loteRepository.contarColetadosHoje();

        // Contagem por fonte
        Map<String, Long> porFonte = loteRepository.contarPorFonte()
            .stream().collect(Collectors.toMap(
                r -> (String)  r[0],
                r -> (Long)    r[1]
            ));

        // Contagem por tipo
        Map<String, Long> porTipo = loteRepository.contarPorTipo()
            .stream().collect(Collectors.toMap(
                r -> r[0].toString(),
                r -> (Long) r[1]
            ));

        // Score mediano
        BigDecimal scoreMediano = loteRepository.scoreMediano();

        // Maior desconto
        BigDecimal maiorDesconto = loteRepository.findAll().stream()
            .filter(l -> l.getDescontoPercentual() != null)
            .max(Comparator.comparing(Lote::getDescontoPercentual))
            .map(Lote::getDescontoPercentual)
            .orElse(null);

        // Data da última coleta
        LocalDateTime ultimaColeta = loteRepository.findTopByOrderByColetadoEmDesc()
            .map(Lote::getColetadoEm)
            .orElse(null);

        return new ResumoDTO(
            total, disponiveis, comScore, hoje,
            porFonte, porTipo, scoreMediano, maiorDesconto, ultimaColeta
        );
    }

    // ── Score ─────────────────────────────────────────────────────────────────

    /** Força o recálculo de score de todos os lotes. */
    @Transactional
    public int recalcularScore() {
        log.info("[api] Recálculo de score solicitado via API.");
        return motorScore.recalcularTodos();
    }

    /** Calcula o score de um lote específico. */
    @Transactional
    public LoteDTO calcularScoreLote(Long id) {
        Lote lote = loteRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Lote não encontrado: " + id));
        motorScore.calcularESalvar(lote);
        return LoteDTO.from(lote);
    }
}
