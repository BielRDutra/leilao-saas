"use client";

import { useState, useCallback } from "react";
import useSWR from "swr";
import { Search } from "lucide-react";
import { buscarLotes } from "@/lib/api";
import { LoteCard } from "@/components/ui/LoteCard";
import { FiltrosBuscaPanel } from "@/components/ui/FiltrosBusca";
import type { FiltrosBusca, RankingItemDTO } from "@/types";

export default function BuscarPage() {
  const [filtros, setFiltros]   = useState<FiltrosBusca>({});
  const [pagina, setPagina]     = useState(0);

  const chave = ["buscar", filtros, pagina];

  const { data, error, isLoading } = useSWR(
    chave,
    () => buscarLotes(filtros, pagina, 24),
    { keepPreviousData: true }
  );

  const handleFiltros = useCallback((novo: FiltrosBusca) => {
    setFiltros(novo);
    setPagina(0);
  }, []);

  // Adapta LoteDTO → RankingItemDTO para reutilizar LoteCard
  const lotes: RankingItemDTO[] = (data?.content ?? []).map((l) => ({
    id:                  l.id,
    fonte:               l.fonte,
    urlOriginal:         l.urlOriginal,
    tipo:                l.tipo,
    cidade:              l.cidade,
    estado:              l.estado,
    valorLanceInicial:   l.valorLanceInicial,
    valorAvaliacao:      l.valorAvaliacao,
    descontoPercentual:  l.descontoPercentual,
    aceitaFinanciamento: l.aceitaFinanciamento,
    aceitaFgts:          l.aceitaFgts,
    scoreOportunidade:   l.scoreOportunidade,
    classificacao:       l.classificacao,
    dataLeilao:          l.dataLeilao,
  }));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

      {/* Header */}
      <div className="flex items-center gap-2">
        <Search className="w-5 h-5 text-brand-500" />
        <h1 className="text-2xl font-bold text-gray-900">Buscar leilões</h1>
      </div>

      {/* Filtros */}
      <FiltrosBuscaPanel value={filtros} onChange={handleFiltros} loading={isLoading} />

      {/* Resultado */}
      {data && (
        <p className="text-sm text-gray-400">
          {data.totalElements} lotes encontrados
          {isLoading && " · atualizando..."}
        </p>
      )}

      {/* Erro */}
      {error && (
        <div className="card p-8 text-center text-gray-400">
          <p>Erro ao buscar lotes. Verifique a conexão com a API.</p>
        </div>
      )}

      {/* Esqueleto */}
      {isLoading && !data && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="card p-5 h-52 animate-pulse">
              <div className="h-3 bg-gray-100 rounded w-1/2 mb-3" />
              <div className="h-2 bg-gray-100 rounded w-full mb-2" />
            </div>
          ))}
        </div>
      )}

      {/* Grid */}
      {lotes.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {lotes.map((lote) => (
            <LoteCard key={lote.id} lote={lote} />
          ))}
        </div>
      )}

      {/* Vazio */}
      {!isLoading && !error && data?.content.length === 0 && (
        <div className="card p-8 text-center text-gray-400">
          <p>Nenhum lote encontrado com os filtros aplicados.</p>
        </div>
      )}

      {/* Paginação */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            className="btn-ghost text-sm"
            disabled={data.first}
            onClick={() => setPagina((p) => p - 1)}
          >
            ← Anterior
          </button>
          <span className="text-sm text-gray-500">
            Página {data.number + 1} de {data.totalPages}
          </span>
          <button
            className="btn-ghost text-sm"
            disabled={data.last}
            onClick={() => setPagina((p) => p + 1)}
          >
            Próxima →
          </button>
        </div>
      )}
    </div>
  );
}
