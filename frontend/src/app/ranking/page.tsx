"use client";

import useSWR from "swr";
import { Trophy, RefreshCw } from "lucide-react";
import { getRanking } from "@/lib/api";
import { LoteCard } from "@/components/ui/LoteCard";

export default function RankingPage() {
  const { data, error, isLoading, mutate } = useSWR(
    "ranking",
    () => getRanking(100, 0, 100),
    { refreshInterval: 5 * 60 * 1000 } // revalida a cada 5 min
  );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Trophy className="w-5 h-5 text-amber-500" />
            <h1 className="text-2xl font-bold text-gray-900">Melhores oportunidades</h1>
          </div>
          <p className="text-gray-500 text-sm">
            Lotes ranqueados pelo score de oportunidade — desconto, financiamento, localização e risco.
          </p>
        </div>
        <button
          className="btn-ghost text-sm"
          onClick={() => mutate()}
          disabled={isLoading}
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
          Atualizar
        </button>
      </div>

      {/* Total */}
      {data && (
        <p className="text-sm text-gray-400 mb-4">
          {data.totalElements} lotes disponíveis
        </p>
      )}

      {/* Estado: loading */}
      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="card p-5 h-52 animate-pulse">
              <div className="h-3 bg-gray-100 rounded w-1/2 mb-3" />
              <div className="h-2 bg-gray-100 rounded w-full mb-2" />
              <div className="h-2 bg-gray-100 rounded w-3/4" />
            </div>
          ))}
        </div>
      )}

      {/* Estado: erro */}
      {error && (
        <div className="card p-8 text-center text-gray-400">
          <p className="text-lg mb-2">Não foi possível carregar os lotes.</p>
          <p className="text-sm">Verifique se a API está rodando em localhost:8080</p>
          <button className="btn-primary mt-4" onClick={() => mutate()}>
            Tentar novamente
          </button>
        </div>
      )}

      {/* Estado: vazio */}
      {!isLoading && !error && data?.content.length === 0 && (
        <div className="card p-8 text-center text-gray-400">
          <p className="text-lg mb-2">Nenhum lote com score disponível.</p>
          <p className="text-sm">Execute a coleta e o cálculo de score primeiro.</p>
        </div>
      )}

      {/* Grid de lotes */}
      {data && data.content.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {data.content.map((lote) => (
            <LoteCard key={lote.id} lote={lote} />
          ))}
        </div>
      )}
    </div>
  );
}
