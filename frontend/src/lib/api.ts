import axios from "axios";
import type { FiltrosBusca, LoteDTO, Page, RankingItemDTO, ResumoDTO } from "@/types";

// ── Cliente base ──────────────────────────────────────────────────────────────

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api",
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
});

// Interceptor de erro global
api.interceptors.response.use(
  (res) => res,
  (err) => {
    console.error("[api]", err?.response?.status, err?.response?.data ?? err.message);
    return Promise.reject(err);
  }
);

// ── Endpoints de lotes ────────────────────────────────────────────────────────

export async function getRanking(
  limite = 100,
  page = 0,
  size = 20
): Promise<Page<RankingItemDTO>> {
  const { data } = await api.get("/v1/lotes/ranking", {
    params: { limite, page, size },
  });
  return data;
}

export async function buscarLotes(
  filtros: FiltrosBusca,
  page = 0,
  size = 20
): Promise<Page<LoteDTO>> {
  const { data } = await api.get("/v1/lotes/buscar", {
    params: { ...filtros, page, size },
  });
  return data;
}

export async function getLote(id: number): Promise<LoteDTO> {
  const { data } = await api.get(`/v1/lotes/${id}`);
  return data;
}

export async function getResumo(): Promise<ResumoDTO> {
  const { data } = await api.get("/v1/lotes/resumo");
  return data;
}

export async function recalcularScore(): Promise<{ lotesPontuados: number }> {
  const { data } = await api.post("/v1/score/recalcular");
  return data;
}

// ── Utilitários ───────────────────────────────────────────────────────────────

export function formatarMoeda(valor: number | null | undefined): string {
  if (valor == null) return "—";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(valor);
}

export function formatarData(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit", month: "2-digit", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  }).format(new Date(iso));
}

export function formatarDesconto(pct: number | null | undefined): string {
  if (pct == null) return "—";
  return `${pct.toFixed(1)}% off`;
}
