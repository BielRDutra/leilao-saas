package com.leilao.alerta;

import com.leilao.model.Lote;
import com.leilao.model.TipoLote;
import com.leilao.repository.LoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock AssinanteRepository       assinanteRepo;
    @Mock HistoricoAlertaRepository historicoRepo;
    @Mock LoteRepository            loteRepo;
    @Mock EmailService              emailService;
    @Mock WhatsAppService           whatsAppService;

    @InjectMocks AlertaService alertaService;

    private Lote           loteBom;
    private Assinante      assinanteCompleto;

    @BeforeEach
    void setUp() {
        loteBom = Lote.builder()
            .id(1L).fonte("caixa").idExterno("001").urlOriginal("http://x.com")
            .tipo(TipoLote.IMOVEL_RESIDENCIAL)
            .valorLanceInicial(new BigDecimal("300000"))
            .valorAvaliacao(new BigDecimal("500000"))
            .scoreOportunidade(new BigDecimal("75.0"))
            .cidade("São Paulo").estado("SP")
            .aceitaFinanciamento(true).aceitaFgts(true)
            .coletadoEm(LocalDateTime.now())
            .build();

        assinanteCompleto = Assinante.builder()
            .id(10L).nome("João")
            .email("joao@gmail.com").whatsapp("5511999990000")
            .scoreMinimo(new BigDecimal("60.0"))
            .estado("SP").ativo(true)
            .build();
    }

    @Test
    void processarLote_scoreAbaixoDoThreshold_naoEnvia() {
        Assinante exigente = Assinante.builder()
            .id(20L).email("x@x.com").ativo(true)
            .scoreMinimo(new BigDecimal("80.0")).build();

        when(assinanteRepo.buscarAssinantesParaLote(any(), any(), any()))
            .thenReturn(List.of(exigente));

        int enviados = alertaService.processarLote(loteBom);

        assertThat(enviados).isZero();
        verify(emailService, never()).enviarAlerta(any(), any());
        verify(whatsAppService, never()).enviarAlerta(any(), any());
    }

    @Test
    void processarLote_scoreAcima_enviaPorAmbosCanais() {
        when(assinanteRepo.buscarAssinantesParaLote(any(), any(), any()))
            .thenReturn(List.of(assinanteCompleto));
        when(historicoRepo.existsByAssinanteIdAndLoteIdAndCanal(any(), any(), any()))
            .thenReturn(false);
        when(emailService.enviarAlerta(any(), any())).thenReturn(true);
        when(whatsAppService.enviarAlerta(any(), any())).thenReturn(true);

        int enviados = alertaService.processarLote(loteBom);

        assertThat(enviados).isEqualTo(2);
        verify(emailService).enviarAlerta(assinanteCompleto, loteBom);
        verify(whatsAppService).enviarAlerta(assinanteCompleto, loteBom);
    }

    @Test
    void processarLote_jaEnviado_naoReenvia() {
        when(assinanteRepo.buscarAssinantesParaLote(any(), any(), any()))
            .thenReturn(List.of(assinanteCompleto));
        when(historicoRepo.existsByAssinanteIdAndLoteIdAndCanal(any(), any(), any()))
            .thenReturn(true);

        int enviados = alertaService.processarLote(loteBom);

        assertThat(enviados).isZero();
        verify(emailService, never()).enviarAlerta(any(), any());
        verify(whatsAppService, never()).enviarAlerta(any(), any());
    }

    @Test
    void processarLote_semScore_ignoraLote() {
        loteBom.setScoreOportunidade(null);

        int enviados = alertaService.processarLote(loteBom);

        assertThat(enviados).isZero();
        verify(assinanteRepo, never()).buscarAssinantesParaLote(any(), any(), any());
    }

    @Test
    void processarLote_falhaEmail_registraNoHistorico() {
        when(assinanteRepo.buscarAssinantesParaLote(any(), any(), any()))
            .thenReturn(List.of(assinanteCompleto));
        when(historicoRepo.existsByAssinanteIdAndLoteIdAndCanal(any(), any(), any()))
            .thenReturn(false);
        when(emailService.enviarAlerta(any(), any())).thenReturn(false);
        when(whatsAppService.enviarAlerta(any(), any())).thenReturn(true);

        int enviados = alertaService.processarLote(loteBom);

        assertThat(enviados).isEqualTo(1);
        // Histórico salvo para e-mail (falha) e WhatsApp (sucesso)
        verify(historicoRepo, times(2)).save(any(HistoricoAlerta.class));
    }

    @Test
    void processarLote_assinanteSoEmail_enviaSoEmail() {
        Assinante soEmail = Assinante.builder()
            .id(30L).email("a@a.com").whatsapp(null)
            .scoreMinimo(new BigDecimal("50.0")).ativo(true).build();

        when(assinanteRepo.buscarAssinantesParaLote(any(), any(), any()))
            .thenReturn(List.of(soEmail));
        when(historicoRepo.existsByAssinanteIdAndLoteIdAndCanal(any(), any(), any()))
            .thenReturn(false);
        when(emailService.enviarAlerta(any(), any())).thenReturn(true);

        int enviados = alertaService.processarLote(loteBom);

        assertThat(enviados).isEqualTo(1);
        verify(emailService).enviarAlerta(soEmail, loteBom);
        verify(whatsAppService, never()).enviarAlerta(any(), any());
    }

    @Test
    void processarAlertasDiarios_semLotes_retornaZero() {
        when(loteRepo.findColetadosHoje()).thenReturn(List.of());

        int enviados = alertaService.processarAlertasDiarios();

        assertThat(enviados).isZero();
        verify(assinanteRepo, never()).buscarAssinantesParaLote(any(), any(), any());
    }
}
