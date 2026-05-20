package com.leilao.score;

import com.leilao.config.ScoreConfig;
import com.leilao.model.Lote;
import com.leilao.model.OrigemLeilao;
import com.leilao.model.StatusLote;
import com.leilao.model.TipoLote;
import com.leilao.repository.LoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorScoreTest {

    @Mock
    private LoteRepository loteRepository;

    private ScoreConfig config;
    private MotorScore  motor;

    @BeforeEach
    void setUp() {
        config = new ScoreConfig();   // usa os defaults do application.properties
        motor  = new MotorScore(config, loteRepository);
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    private Lote loteBase() {
        return Lote.builder()
            .fonte("teste").idExterno("001")
            .urlOriginal("http://x.com/lote/001")
            .tipo(TipoLote.IMOVEL_RESIDENCIAL)
            .status(StatusLote.DISPONIVEL)
            .valorAvaliacao(new BigDecimal("500000"))
            .valorLanceInicial(new BigDecimal("300000"))
            .aceitaFinanciamento(true)
            .aceitaFgts(true)
            .bancoFinanciador("Caixa Econômica Federal")
            .cidade("São Paulo").estado("SP").bairro("Moema")
            .origem(OrigemLeilao.EXTRAJUDICIAL)
            .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 1 — Desconto
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class ScoreDescontoTest {
        private DimensoesScore.ScoreDesconto dim;

        @BeforeEach void setUp() { dim = new DimensoesScore.ScoreDesconto(config); }

        @Test void semAvaliacao_retornaZero() {
            Lote l = loteBase(); l.setValorAvaliacao(null);
            assertThat(dim.calcular(l).valor()).isEqualTo(0.0);
        }

        @Test void lanceIgualAvaliacao_retornaZero() {
            Lote l = loteBase();
            l.setValorAvaliacao(new BigDecimal("300000"));
            assertThat(dim.calcular(l).valor()).isEqualTo(0.0);
        }

        @Test void descontoAbaixoMinimo_retornaZero() {
            // 5% de desconto, mínimo é 10%
            Lote l = loteBase();
            l.setValorAvaliacao(new BigDecimal("100000"));
            l.setValorLanceInicial(new BigDecimal("95000"));
            assertThat(dim.calcular(l).valor()).isEqualTo(0.0);
        }

        @Test void descontoMaximo_retornaCem() {
            // 50% ou mais → 100
            Lote l = loteBase();
            l.setValorAvaliacao(new BigDecimal("500000"));
            l.setValorLanceInicial(new BigDecimal("200000"));
            assertThat(dim.calcular(l).valor()).isEqualTo(100.0);
        }

        @Test void desconto30pct_entre50e75() {
            Lote l = loteBase();
            l.setValorAvaliacao(new BigDecimal("100000"));
            l.setValorLanceInicial(new BigDecimal("70000"));
            double v = dim.calcular(l).valor();
            assertThat(v).isBetween(50.0, 75.0);
        }

        @Test void curvaNaoLinear_40pctMaisQue2xde20pct() {
            Lote l20 = loteBase();
            l20.setValorAvaliacao(new BigDecimal("100000"));
            l20.setValorLanceInicial(new BigDecimal("80000"));

            Lote l40 = loteBase();
            l40.setValorAvaliacao(new BigDecimal("100000"));
            l40.setValorLanceInicial(new BigDecimal("60000"));

            double v20 = dim.calcular(l20).valor();
            double v40 = dim.calcular(l40).valor();
            assertThat(v40).isGreaterThan(v20 * 1.5);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 2 — Financiamento
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class ScoreFinanciamentoTest {
        private DimensoesScore.ScoreFinanciamento dim;

        @BeforeEach void setUp() { dim = new DimensoesScore.ScoreFinanciamento(config); }

        @Test void semOpcoes_retornaZero() {
            Lote l = loteBase();
            l.setAceitaFinanciamento(false); l.setAceitaFgts(false); l.setBancoFinanciador(null);
            assertThat(dim.calcular(l).valor()).isEqualTo(0.0);
        }

        @Test void apenasFinanciamento_retornaBonusFinanciamento() {
            Lote l = loteBase();
            l.setAceitaFinanciamento(true); l.setAceitaFgts(false); l.setBancoFinanciador(null);
            assertThat(dim.calcular(l).valor()).isEqualTo(config.getBonusFinanciamento());
        }

        @Test void financiamentoEFgts() {
            Lote l = loteBase();
            l.setAceitaFinanciamento(true); l.setAceitaFgts(true); l.setBancoFinanciador(null);
            assertThat(dim.calcular(l).valor())
                .isEqualTo(config.getBonusFinanciamento() + config.getBonusFgts());
        }

        @Test void tudoComCaixa_capEm100() {
            Lote l = loteBase();
            l.setAceitaFinanciamento(true); l.setAceitaFgts(true);
            l.setBancoFinanciador("Caixa Econômica Federal");
            assertThat(dim.calcular(l).valor()).isLessThanOrEqualTo(100.0);
        }

        @Test void bancoNaoCaixa_semBonusCaixa() {
            Lote l = loteBase();
            l.setAceitaFinanciamento(true); l.setAceitaFgts(false);
            l.setBancoFinanciador("Banco do Brasil");
            assertThat(dim.calcular(l).valor()).isEqualTo(config.getBonusFinanciamento());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 3 — Localização
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class ScoreLocalizacaoTest {
        private DimensoesScore.ScoreLocalizacao dim;

        @BeforeEach void setUp() { dim = new DimensoesScore.ScoreLocalizacao(config); }

        @Test void semLocalizacao_retornaZero() {
            Lote l = loteBase();
            l.setCidade(null); l.setEstado(null); l.setBairro(null);
            assertThat(dim.calcular(l).valor()).isEqualTo(0.0);
        }

        @Test void estadoECidadePremium_retorna80() {
            Lote l = loteBase();
            l.setCidade("São Paulo"); l.setEstado("SP"); l.setBairro(null);
            l.setLatitude(null); l.setLongitude(null);
            // Base 40 + estado 20 + cidade 20 = 80
            assertThat(dim.calcular(l).valor()).isEqualTo(80.0);
        }

        @Test void estadoNaoPremium_retorna40() {
            Lote l = loteBase();
            l.setCidade("Manaus"); l.setEstado("AM"); l.setBairro(null);
            l.setLatitude(null); l.setLongitude(null);
            assertThat(dim.calcular(l).valor()).isEqualTo(40.0);
        }

        @Test void comBairroECoordenadas_retorna100() {
            Lote l = loteBase();
            l.setCidade("São Paulo"); l.setEstado("SP"); l.setBairro("Moema");
            l.setLatitude(new BigDecimal("-23.5")); l.setLongitude(new BigDecimal("-46.6"));
            // 40 + 20 + 20 + 10 + 10 = 100
            assertThat(dim.calcular(l).valor()).isEqualTo(100.0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DIMENSÃO 4 — Risco
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    class ScoreRiscoTest {
        private DimensoesScore.ScoreRisco dim;

        @BeforeEach void setUp() { dim = new DimensoesScore.ScoreRisco(config); }

        @Test void semRiscos_retornaCem() {
            Lote l = loteBase();
            l.setOcupado(false); l.setDataSegundoLeilao(null);
            assertThat(dim.calcular(l).valor()).isEqualTo(100.0);
        }

        @Test void ocupado_penaliza40() {
            Lote l = loteBase();
            l.setOcupado(true); l.setOrigem(OrigemLeilao.EXTRAJUDICIAL);
            assertThat(dim.calcular(l).valor())
                .isEqualTo(100.0 - config.getPenaltOcupado());
        }

        @Test void semAvaliacao_penaliza20() {
            Lote l = loteBase();
            l.setValorAvaliacao(null); l.setOcupado(false);
            assertThat(dim.calcular(l).valor())
                .isEqualTo(100.0 - config.getPenaltSemAvaliacao());
        }

        @Test void judicial_ganhaBonusDe10() {
            Lote l = loteBase();
            l.setOcupado(false); l.setOrigem(OrigemLeilao.JUDICIAL);
            assertThat(dim.calcular(l).valor())
                .isEqualTo(100.0 + config.getBonusJudicial());
        }

        @Test void multiplasPenalidades_naoFicaAbaixoDeZero() {
            Lote l = loteBase();
            l.setOcupado(true); l.setValorAvaliacao(null);
            l.setDataPrimeiroLeilao(LocalDateTime.now().minusDays(30));
            l.setDataSegundoLeilao(LocalDateTime.now());
            l.setOrigem(OrigemLeilao.EXTRAJUDICIAL);
            assertThat(dim.calcular(l).valor()).isGreaterThanOrEqualTo(0.0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MOTOR COMPLETO
    // ══════════════════════════════════════════════════════════════════════════

    @Test void scoreFinal_entre0e100() {
        ResultadoScore r = motor.calcular(loteBase());
        assertThat(r.scoreFinal()).isBetween(0.0, 100.0);
    }

    @Test void loteExcelente_scoreAlto() {
        Lote l = loteBase();
        l.setValorLanceInicial(new BigDecimal("275000")); // 45% off
        l.setLatitude(new BigDecimal("-23.56"));
        l.setLongitude(new BigDecimal("-46.68"));
        l.setOcupado(false);

        ResultadoScore r = motor.calcular(l);
        assertThat(r.scoreFinal()).isGreaterThanOrEqualTo(70.0);
        assertThat(r.classificacao()).isIn("Excelente", "Muito bom");
    }

    @Test void lotePessimo_scoreBaixo() {
        Lote l = Lote.builder()
            .fonte("teste").idExterno("999").urlOriginal("http://x.com")
            .valorAvaliacao(new BigDecimal("500000"))
            .valorLanceInicial(new BigDecimal("490000"))  // 2% off
            .aceitaFinanciamento(false).aceitaFgts(false)
            .ocupado(true)
            .origem(OrigemLeilao.EXTRAJUDICIAL)
            .build();

        ResultadoScore r = motor.calcular(l);
        assertThat(r.scoreFinal()).isLessThan(30.0);
        assertThat(r.classificacao()).isEqualTo("Baixo");
    }

    @Test void resultado_tem4Dimensoes() {
        ResultadoScore r = motor.calcular(loteBase());
        assertThat(r.dimensoes()).hasSize(4);
        assertThat(r.dimensoes().stream().map(ResultadoDimensao::nome))
            .containsExactlyInAnyOrder("desconto", "financiamento", "localizacao", "risco");
    }

    @Test void pesosDaConfigSomam1() {
        double total = config.getPesoDesconto()
            + config.getPesoFinanciamento()
            + config.getPesoLocalizacao()
            + config.getPesoRisco();
        assertThat(total).isCloseTo(1.0, within(0.001));
    }

    @Test void calcularESalvar_persisteNoRepositorio() {
        when(loteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Lote lote = loteBase();

        ResultadoScore r = motor.calcularESalvar(lote);

        verify(loteRepository, times(1)).save(lote);
        assertThat(lote.getScoreOportunidade()).isNotNull();
        assertThat(lote.getScoreCalculadoEm()).isNotNull();
        assertThat(r.scoreFinal()).isBetween(0.0, 100.0);
    }

    @Test void classificacoes_corretas() {
        assertThat(classificacaoParaScore(85.0)).isEqualTo("Excelente");
        assertThat(classificacaoParaScore(70.0)).isEqualTo("Muito bom");
        assertThat(classificacaoParaScore(55.0)).isEqualTo("Bom");
        assertThat(classificacaoParaScore(40.0)).isEqualTo("Regular");
        assertThat(classificacaoParaScore(20.0)).isEqualTo("Baixo");
    }

    private String classificacaoParaScore(double score) {
        ResultadoScore r = new ResultadoScore(
            null, "t", "1", score, score, score, score, score,
            java.util.List.of(), LocalDateTime.now());
        return r.classificacao();
    }
}
