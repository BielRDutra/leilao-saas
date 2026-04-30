"use client";

import Link from "next/link";
import {
  MapPin, Calendar, TrendingDown, Banknote,
  CheckCircle2, Building2, Car, Landmark,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { formatarMoeda, formatarData, formatarDesconto } from "@/lib/api";
import { CLASSIFICACAO_COR, FONTE_LABEL, TIPO_LABEL, type RankingItemDTO } from "@/types";

interface Props {
  lote: RankingItemDTO;
}

const TIPO_ICONE: Record<string, React.ReactNode> = {
  IMOVEL_RESIDENCIAL: <Building2 className="w-4 h-4" />,
  IMOVEL_COMERCIAL:   <Landmark  className="w-4 h-4" />,
  IMOVEL_RURAL:       <Building2 className="w-4 h-4" />,
  VEICULO:            <Car       className="w-4 h-4" />,
};

export function LoteCard({ lote }: Props) {
  const scoreCor = lote.scoreOportunidade != null
    ? lote.scoreOportunidade >= 65 ? "text-emerald-600"
    : lote.scoreOportunidade >= 45 ? "text-blue-600"
    : "text-gray-500"
    : "text-gray-400";

  return (
    <Link href={`/lote/${lote.id}`} className="block group">
      <div className="card p-5 hover:shadow-md hover:border-brand-100 transition-all duration-200 h-full flex flex-col gap-3">

        {/* Header: tipo + fonte + classificação */}
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-1.5 text-gray-500">
            {TIPO_ICONE[lote.tipo] ?? <Building2 className="w-4 h-4" />}
            <span className="text-xs font-medium">{TIPO_LABEL[lote.tipo]}</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-xs text-gray-400">
              {FONTE_LABEL[lote.fonte] ?? lote.fonte}
            </span>
            <span className={cn(
              "badge",
              CLASSIFICACAO_COR[lote.classificacao] ?? "bg-gray-100 text-gray-500"
            )}>
              {lote.classificacao}
            </span>
          </div>
        </div>

        {/* Score */}
        {lote.scoreOportunidade != null && (
          <div className="flex items-center gap-2">
            <div className="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
              <div
                className={cn(
                  "h-full rounded-full transition-all",
                  lote.scoreOportunidade >= 65 ? "bg-emerald-500"
                  : lote.scoreOportunidade >= 45 ? "bg-blue-500"
                  : "bg-gray-300"
                )}
                style={{ width: `${lote.scoreOportunidade}%` }}
              />
            </div>
            <span className={cn("text-sm font-semibold tabular-nums", scoreCor)}>
              {lote.scoreOportunidade.toFixed(0)}
            </span>
          </div>
        )}

        {/* Localização */}
        {(lote.cidade || lote.estado) && (
          <div className="flex items-center gap-1 text-gray-500">
            <MapPin className="w-3.5 h-3.5 flex-shrink-0" />
            <span className="text-xs truncate">
              {[lote.cidade, lote.estado].filter(Boolean).join(" / ")}
            </span>
          </div>
        )}

        {/* Valores */}
        <div className="mt-auto pt-2 border-t border-gray-50">
          <div className="flex items-end justify-between">
            <div>
              <p className="text-xs text-gray-400 mb-0.5">Lance inicial</p>
              <p className="text-lg font-semibold text-gray-900 tabular-nums">
                {formatarMoeda(lote.valorLanceInicial)}
              </p>
            </div>
            <div className="text-right">
              {lote.descontoPercentual != null && (
                <div className="flex items-center gap-1 text-emerald-600">
                  <TrendingDown className="w-3.5 h-3.5" />
                  <span className="text-sm font-medium">
                    {formatarDesconto(lote.descontoPercentual)}
                  </span>
                </div>
              )}
              {lote.valorAvaliacao && (
                <p className="text-xs text-gray-400 line-through tabular-nums">
                  {formatarMoeda(lote.valorAvaliacao)}
                </p>
              )}
            </div>
          </div>

          {/* Tags de financiamento */}
          <div className="flex items-center gap-1.5 mt-2 flex-wrap">
            {lote.aceitaFinanciamento && (
              <span className="badge bg-blue-50 text-blue-700">
                <CheckCircle2 className="w-3 h-3" /> Financiamento
              </span>
            )}
            {lote.aceitaFgts && (
              <span className="badge bg-purple-50 text-purple-700">
                <Banknote className="w-3 h-3" /> FGTS
              </span>
            )}
            {lote.dataLeilao && (
              <span className="badge bg-gray-50 text-gray-500 ml-auto">
                <Calendar className="w-3 h-3" />
                {formatarData(lote.dataLeilao)}
              </span>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
}
