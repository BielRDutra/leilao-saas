package com.leilao.alerta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leilao.alerta.dto.AssinanteRespostaDTO;
import com.leilao.alerta.dto.CriarAssinanteDTO;
import com.leilao.alerta.service.AlertaFacadeService;
import com.leilao.model.TipoLote;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertaController.class)
class AlertaControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean AlertaFacadeService facadeService;

    private AssinanteRespostaDTO respostaDTO() {
        return new AssinanteRespostaDTO(
            1L, "João", "jo**@gmail.com", "5511****0000",
            true, new BigDecimal("65.0"),
            "São Paulo", "SP", TipoLote.IMOVEL_RESIDENCIAL,
            LocalDateTime.now()
        );
    }

    // ── POST /assinantes ──────────────────────────────────────────────────────

    @Test
    void cadastrar_dadosValidos_retorna201() throws Exception {
        when(facadeService.cadastrarAssinante(any())).thenReturn(respostaDTO());

        CriarAssinanteDTO dto = new CriarAssinanteDTO(
            "João", "joao@gmail.com", "5511999990000",
            new BigDecimal("65.0"), "São Paulo", "SP",
            TipoLote.IMOVEL_RESIDENCIAL
        );

        mockMvc.perform(post("/api/v1/alertas/assinantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("jo**@gmail.com"))
            .andExpect(jsonPath("$.whatsapp").value("5511****0000"));
    }

    @Test
    void cadastrar_emailInvalido_retorna400() throws Exception {
        CriarAssinanteDTO dto = new CriarAssinanteDTO(
            "João", "email-invalido", null,
            new BigDecimal("65.0"), null, null, null
        );

        mockMvc.perform(post("/api/v1/alertas/assinantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_scoreMinimoNulo_retorna400() throws Exception {
        CriarAssinanteDTO dto = new CriarAssinanteDTO(
            "João", "joao@gmail.com", null,
            null, null, null, null // scoreMinimo null
        );

        mockMvc.perform(post("/api/v1/alertas/assinantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_whatsappFormatoInvalido_retorna400() throws Exception {
        CriarAssinanteDTO dto = new CriarAssinanteDTO(
            "João", null, "123", // whatsapp curto demais
            new BigDecimal("65.0"), null, null, null
        );

        mockMvc.perform(post("/api/v1/alertas/assinantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    // ── GET /assinantes ───────────────────────────────────────────────────────

    @Test
    void listar_retorna200ComLista() throws Exception {
        when(facadeService.listarAssinantes()).thenReturn(List.of(respostaDTO()));

        mockMvc.perform(get("/api/v1/alertas/assinantes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].scoreMinimo").value(65.0));
    }

    @Test
    void listar_semAssinantes_retornaListaVazia() throws Exception {
        when(facadeService.listarAssinantes()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alertas/assinantes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    // ── DELETE /assinantes/{id} ───────────────────────────────────────────────

    @Test
    void cancelar_idExistente_retorna200() throws Exception {
        mockMvc.perform(delete("/api/v1/alertas/assinantes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value("Assinante cancelado com sucesso."));
    }

    @Test
    void cancelar_idInexistente_retorna404() throws Exception {
        doThrow(new EntityNotFoundException("Assinante não encontrado: 99"))
            .when(facadeService).cancelarAssinante(99L);

        mockMvc.perform(delete("/api/v1/alertas/assinantes/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    // ── POST /processar ───────────────────────────────────────────────────────

    @Test
    void processar_retornaQuantidadeEnviada() throws Exception {
        when(facadeService.processarAlertasDiarios()).thenReturn(42);

        mockMvc.perform(post("/api/v1/alertas/processar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificacoesEnviadas").value(42));
    }

    // ── POST /processar/lote/{id} ─────────────────────────────────────────────

    @Test
    void processarLote_loteValido_retorna200() throws Exception {
        when(facadeService.processarLote(1L)).thenReturn(3);

        mockMvc.perform(post("/api/v1/alertas/processar/lote/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificacoesEnviadas").value(3));
    }

    @Test
    void processarLote_loteInexistente_retorna404() throws Exception {
        when(facadeService.processarLote(99L))
            .thenThrow(new EntityNotFoundException("Lote não encontrado: 99"));

        mockMvc.perform(post("/api/v1/alertas/processar/lote/99"))
            .andExpect(status().isNotFound());
    }
}
