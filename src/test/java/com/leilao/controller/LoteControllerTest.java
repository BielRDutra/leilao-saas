package com.leilao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leilao.dto.LoteDTO;
import com.leilao.dto.RankingItemDTO;
import com.leilao.dto.ResumoDTO;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;
import com.leilao.service.LoteService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoteController.class)
class LoteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean LoteService loteService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private RankingItemDTO itemRanking() {
        return new RankingItemDTO(
            1L, "caixa", "https://caixa.gov.br/lote/1",
            TipoLote.IMOVEL_RESIDENCIAL,
            "São Paulo", "SP",
            new BigDecimal("280000"),
            new BigDecimal("500000"),
            new BigDecimal("44.00"),
            true, true,
            new BigDecimal("78.50"),
            "Muito bom",
            LocalDateTime.now().plusDays(10)
        );
    }

    private LoteDTO loteDTO() {
        return new LoteDTO(
            1L, "caixa", "https://caixa.gov.br/lote/1",
            TipoLote.IMOVEL_RESIDENCIAL, null, StatusLote.DISPONIVEL,
            new BigDecimal("500000"), new BigDecimal("280000"), new BigDecimal("44.00"),
            true, true, "Caixa Econômica Federal",
            "Rua das Flores, 100", "Centro", "São Paulo", "SP", "01310-000",
            null, null,
            "Apartamento 2 quartos bem localizado", new BigDecimal("65"),
            false,
            LocalDateTime.now().plusDays(10), null, null,
            new BigDecimal("78.50"), new BigDecimal("90"), new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("100"),
            "Muito bom",
            LocalDateTime.now()
        );
    }

    // ── GET /lotes/ranking ────────────────────────────────────────────────────

    @Test
    void ranking_retornaStatus200() throws Exception {
        when(loteService.ranking(anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(itemRanking())));

        mockMvc.perform(get("/api/v1/lotes/ranking"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content[0].fonte").value("caixa"))
            .andExpect(jsonPath("$.content[0].scoreOportunidade").value(78.50))
            .andExpect(jsonPath("$.content[0].classificacao").value("Muito bom"));
    }

    @Test
    void ranking_comLimiteCustomizado_retornaStatus200() throws Exception {
        when(loteService.ranking(eq(50), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(itemRanking())));

        mockMvc.perform(get("/api/v1/lotes/ranking?limite=50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── GET /lotes/buscar ─────────────────────────────────────────────────────

    @Test
    void buscar_semFiltros_retornaStatus200() throws Exception {
        when(loteService.buscar(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(loteDTO())));

        mockMvc.perform(get("/api/v1/lotes/buscar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].cidade").value("São Paulo"))
            .andExpect(jsonPath("$.content[0].aceitaFinanciamento").value(true));
    }

    @Test
    void buscar_comFiltros_retornaStatus200() throws Exception {
        when(loteService.buscar(any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(loteDTO())));

        mockMvc.perform(get("/api/v1/lotes/buscar")
                .param("cidade", "São Paulo")
                .param("estado", "SP")
                .param("aceitaFinanciamento", "true")
                .param("valorMaximo", "400000"))
            .andExpect(status().isOk());
    }

    @Test
    void buscar_estadoInvalido_retornaStatus400() throws Exception {
        mockMvc.perform(get("/api/v1/lotes/buscar")
                .param("estado", "INVALIDO"))  // deve ser 2 letras
            .andExpect(status().isBadRequest());
    }

    // ── GET /lotes/{id} ───────────────────────────────────────────────────────

    @Test
    void detalhe_loteExistente_retornaStatus200() throws Exception {
        when(loteService.buscarPorId(1L)).thenReturn(loteDTO());

        mockMvc.perform(get("/api/v1/lotes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.descontoPercentual").value(44.00))
            .andExpect(jsonPath("$.scoreOportunidade").value(78.50));
    }

    @Test
    void detalhe_loteInexistente_retornaStatus404() throws Exception {
        when(loteService.buscarPorId(99L))
            .thenThrow(new EntityNotFoundException("Lote não encontrado: 99"));

        mockMvc.perform(get("/api/v1/lotes/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.detail").value("Lote não encontrado: 99"));
    }

    // ── GET /lotes/resumo ─────────────────────────────────────────────────────

    @Test
    void resumo_retornaEstatisticas() throws Exception {
        ResumoDTO resumo = new ResumoDTO(
            1500L, 1200L, 1100L, 48L,
            Map.of("caixa", 800L, "superbid", 400L, "sold", 200L, "parque_leiloes", 100L),
            Map.of("IMOVEL_RESIDENCIAL", 900L, "IMOVEL_COMERCIAL", 300L),
            new BigDecimal("58.40"),
            new BigDecimal("65.00"),
            LocalDateTime.now()
        );
        when(loteService.resumo()).thenReturn(resumo);

        mockMvc.perform(get("/api/v1/lotes/resumo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalLotes").value(1500))
            .andExpect(jsonPath("$.lotesDisponiveis").value(1200))
            .andExpect(jsonPath("$.porFonte.caixa").value(800));
    }

    // ── POST /score/recalcular ────────────────────────────────────────────────

    @Test
    void recalcularScore_retornaQuantidadePontuados() throws Exception {
        when(loteService.recalcularScore()).thenReturn(1150);

        mockMvc.perform(post("/api/v1/score/recalcular"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lotesPontuados").value(1150));
    }

    // ── POST /score/lote/{id} ─────────────────────────────────────────────────

    @Test
    void calcularScoreLote_retornaDTOAtualizado() throws Exception {
        when(loteService.calcularScoreLote(1L)).thenReturn(loteDTO());

        mockMvc.perform(post("/api/v1/score/lote/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scoreOportunidade").value(78.50))
            .andExpect(jsonPath("$.classificacao").value("Muito bom"));
    }

    @Test
    void calcularScoreLote_loteInexistente_retorna404() throws Exception {
        when(loteService.calcularScoreLote(99L))
            .thenThrow(new EntityNotFoundException("Lote não encontrado: 99"));

        mockMvc.perform(post("/api/v1/score/lote/99"))
            .andExpect(status().isNotFound());
    }
}
