// ── Enums (espelham os enums Java) ────────────────────────────────────────────

export type TipoLote =
  | "IMOVEL_RESIDENCIAL"
  | "IMOVEL_COMERCIAL"
  | "IMOVEL_RURAL"
  | "VEICULO"
  | "MAQUINARIO"
  | "OUTROS";

export type OrigemLeilao = "JUDICIAL" | "EXTRAJUDICIAL";
export type StatusLote   = "DISPONIVEL" | "ENCERRADO" | "CANCELADO" | "ARREMATADO";

// ── DTOs (espelham os records Java) ───────────────────────────────────────────

export interface LoteDTO {
  id:                  number;
  fonte:               string;
  urlOriginal:         string;
  tipo:                TipoLote;
  origem:              OrigemLeilao | null;
  status:              StatusLote;
  valorAvaliacao:      number | null;
  valorLanceInicial:   number;
  descontoPercentual:  number | null;
  aceitaFinanciamento: boolean;
  aceitaFgts:          boolean;
  bancoFinanciador:    string | null;
  logradouro:          string | null;
  bairro:              string | null;
  cidade:              string | null;
  estado:              string | null;
  cep:                 string | null;
  latitude:            number | null;
  longitude:           number | null;
  descricao:           string | null;
  areaM2:              number | null;
  ocupado:             boolean | null;
  dataLeilao:          string | null;
  dataPrimeiroLeilao:  string | null;
  dataSegundoLeilao:   string | null;
  scoreOportunidade:   number | null;
  scoreDesconto:       number | null;
  scoreFinanciamento:  number | null;
  scoreLocalizacao:    number | null;
  scoreRisco:          number | null;
  classificacao:       string;
  coletadoEm:          string;
}

export interface RankingItemDTO {
  id:                  number;
  fonte:               string;
  urlOriginal:         string;
  tipo:                TipoLote;
  cidade:              string | null;
  estado:              string | null;
  valorLanceInicial:   number;
  valorAvaliacao:      number | null;
  descontoPercentual:  number | null;
  aceitaFinanciamento: boolean;
  aceitaFgts:          boolean;
  scoreOportunidade:   number | null;
  classificacao:       string;
  dataLeilao:          string | null;
}

export interface ResumoDTO {
  totalLotes:       number;
  lotesDisponiveis: number;
  lotesComScore:    number;
  coletadosHoje:    number;
  porFonte:         Record<string, number>;
  porTipo:          Record<string, number>;
  scoreMediano:     number | null;
  maiorDesconto:    number | null;
  ultimaColeta:     string | null;
}

// ── Paginação ─────────────────────────────────────────────────────────────────

export interface Page<T> {
  content:          T[];
  totalElements:    number;
  totalPages:       number;
  number:           number;  // página atual (zero-indexed)
  size:             number;
  first:            boolean;
  last:             boolean;
}

// ── Filtros de busca ──────────────────────────────────────────────────────────

export interface FiltrosBusca {
  cidade?:              string;
  estado?:              string;
  tipo?:                TipoLote;
  aceitaFinanciamento?: boolean;
  aceitaFgts?:          boolean;
  valorMaximo?:         number;
  scoreMinimo?:         number;
  limite?:              number;
  page?:                number;
  size?:                number;
}

// ── Labels legíveis ───────────────────────────────────────────────────────────

export const TIPO_LABEL: Record<TipoLote, string> = {
  IMOVEL_RESIDENCIAL: "Imóvel Residencial",
  IMOVEL_COMERCIAL:   "Imóvel Comercial",
  IMOVEL_RURAL:       "Imóvel Rural",
  VEICULO:            "Veículo",
  MAQUINARIO:         "Maquinário",
  OUTROS:             "Outros",
};

export const FONTE_LABEL: Record<string, string> = {
  caixa:          "Caixa Econômica",
  superbid:       "Superbid",
  sold:           "Sold",
  parque_leiloes: "Parque dos Leilões",
};

export const CLASSIFICACAO_COR: Record<string, string> = {
  Excelente:  "bg-emerald-100 text-emerald-800",
  "Muito bom":"bg-blue-100 text-blue-800",
  Bom:        "bg-sky-100 text-sky-800",
  Regular:    "bg-yellow-100 text-yellow-800",
  Baixo:      "bg-gray-100 text-gray-600",
  "Sem score":"bg-gray-100 text-gray-400",
};
