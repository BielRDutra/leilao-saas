"use client";

import { use } from "react";
import useSWR from "swr";
import Link from "next/link";
import {
  ArrowLeft, ExternalLink, MapPin, Calendar,
  Banknote, CheckCircle2, AlertTriangle, Building2,
} from "lucide-react";
import { getLote, formatarMoeda, formatarData } from "@/lib/api";
import { ScoreGauge } from "@/components/ui/ScoreGauge";
import { CLASSIFICACAO_COR, FONTE_LABEL, TIPO_LABEL } from "@/types";
import { cn } from "@/lib/utils";

interface Props { params: Promise<{ id: string }> }

function ScoreBarra({ label, valor, cor }: { label: string; valor: number | null; cor: string }) {
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs">
        <span className="text-gray-500">{label}</span>
        <span className="font-medium tabular-nums">{valor?.toFixed(0) ?? "—"}</span>
      </div>
      <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full"
          style={{ width: `${valor ?? 0}%`, background: cor }}
        />
      </div>
    </div>
  );
}

export default function LoteDetalhe({ params }: Props) {
  const { id } = use(params);
  const { data: lote, error, isLoading } = useSWR(
    `lote-${id}`,
    () => getLote(Number(id))
  );

  if (isLoading) return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-4">
      <div className="h-8 bg-gray-100 rounded w-1/3 animate-pulse" />
      <div className="card p-6 h-64 animate-pulse" />
    </div>
  );

  if (error || !lote) return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="card p-8 text-center text-gray-400">
        <p>Lote não encontrado.</p>
        <Link href="/ranking" className="btn-primary mt-4 inline-flex">← Voltar ao ranking</Link>
      </div>
    </div>
  );

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

      {/* Breadcrumb */}
      <Link href="/ranking" className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700">
        <ArrowLeft className="w-4 h-4" /> Voltar ao ranking
      </Link>

      {/* Header */}
      <div className="card p-6">
        <div className="flex flex-col sm:flex-row gap-6">

          {/* Score gauge */}
          <div className="flex-shrink-0 flex flex-col items-center gap-2 sm:border-r sm:border-gray-100 sm:pr-6">
            <ScoreGauge score={lote.scoreOportunidade} size="lg" />
            <span className={cn(
              "badge text-xs",
              CLASSIFICACAO_COR[lote.classificacao] ?? "bg-gray-100 text-gray-500"
            )}>
              {lote.classificacao}
            </span>
          </div>

          {/* Info principal */}
          <div className="flex-1 space-y-3">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="badge bg-gray-100 text-gray-600">
                <Building2 className="w-3 h-3" />
                {TIPO_LABEL[lote.tipo]}
              </span>
              <span className="badge bg-brand-50 text-brand-600">
                {FONTE_LABEL[lote.fonte] ?? lote.fonte}
              </span>
              {lote.origem === "JUDICIAL" && (
                <span className="badge bg-purple-50 text-purple-700">Judicial</span>
              )}
            </div>

            {/* Localização */}
            {lote.logradouro && (
              <div className="flex items-start gap-1.5 text-sm text-gray-600">
                <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0 text-gray-400" />
                <span>{[lote.logradouro, lote.bairro, lote.cidade, lote.estado].filter(Boolean).join(", ")}</span>
              </div>
            )}

            {/* Datas */}
            {lote.dataLeilao && (
              <div className="flex items-center gap-1.5 text-sm text-gray-600">
                <Calendar className="w-4 h-4 text-gray-400" />
                <span>Leilão em {formatarData(lote.dataLeilao)}</span>
              </div>
            )}

            {/* Valores */}
            <div className="flex items-end gap-4 pt-1">
              <div>
                <p className="text-xs text-gray-400">Lance inicial</p>
                <p className="text-2xl font-bold text-gray-900 tabular-nums">
                  {formatarMoeda(lote.valorLanceInicial)}
                </p>
              </div>
              {lote.valorAvaliacao && (
                <div>
                  <p className="text-xs text-gray-400">Avaliação</p>
                  <p className="text-base text-gray-400 line-through tabular-nums">
                    {formatarMoeda(lote.valorAvaliacao)}
                  </p>
                </div>
              )}
              {lote.descontoPercentual && (
                <span className="badge bg-emerald-100 text-emerald-800 text-sm font-semibold mb-1">
                  {lote.descontoPercentual.toFixed(1)}% off
                </span>
              )}
            </div>

            {/* Financiamento */}
            <div className="flex flex-wrap gap-2 pt-1">
              {lote.aceitaFinanciamento && (
                <span className="badge bg-blue-50 text-blue-700">
                  <CheckCircle2 className="w-3 h-3" /> Aceita financiamento
                </span>
              )}
              {lote.aceitaFgts && (
                <span className="badge bg-purple-50 text-purple-700">
                  <Banknote className="w-3 h-3" /> Aceita FGTS
                </span>
              )}
              {lote.bancoFinanciador && (
                <span className="badge bg-gray-50 text-gray-600">
                  {lote.bancoFinanciador}
                </span>
              )}
              {lote.ocupado && (
                <span className="badge bg-amber-50 text-amber-700">
                  <AlertTriangle className="w-3 h-3" /> Imóvel ocupado
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Link original */}
        <div className="mt-4 pt-4 border-t border-gray-100">
          <a
            href={lote.urlOriginal}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-primary inline-flex"
          >
            <ExternalLink className="w-4 h-4" />
            Ver no portal original
          </a>
        </div>
      </div>

      {/* Breakdown do score */}
      {lote.scoreOportunidade != null && (
        <div className="card p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Detalhamento do score</h2>
          <div className="space-y-4">
            <ScoreBarra label="Desconto (40%)"      valor={lote.scoreDesconto}      cor="#10b981" />
            <ScoreBarra label="Financiamento (25%)" valor={lote.scoreFinanciamento} cor="#3b82f6" />
            <ScoreBarra label="Localização (20%)"   valor={lote.scoreLocalizacao}   cor="#8b5cf6" />
            <ScoreBarra label="Risco (15%)"         valor={lote.scoreRisco}         cor="#f59e0b" />
          </div>
          <div className="mt-4 pt-4 border-t border-gray-100 flex justify-between items-center">
            <span className="text-sm text-gray-500">Score final ponderado</span>
            <span className="text-xl font-bold tabular-nums text-gray-900">
              {lote.scoreOportunidade.toFixed(1)}
            </span>
          </div>
        </div>
      )}

      {/* Descrição */}
      {lote.descricao && (
        <div className="card p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-3">Descrição</h2>
          <p className="text-sm text-gray-600 leading-relaxed whitespace-pre-wrap">
            {lote.descricao}
          </p>
        </div>
      )}

      {/* Dados adicionais */}
      <div className="card p-6">
        <h2 className="text-base font-semibold text-gray-900 mb-3">Dados adicionais</h2>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm">
          {lote.areaM2 && (
            <>
              <dt className="text-gray-500">Área</dt>
              <dd className="text-gray-900 font-medium">{lote.areaM2} m²</dd>
            </>
          )}
          {lote.cep && (
            <>
              <dt className="text-gray-500">CEP</dt>
              <dd className="text-gray-900 font-medium">{lote.cep}</dd>
            </>
          )}
          <dt className="text-gray-500">Coletado em</dt>
          <dd className="text-gray-900 font-medium">{formatarData(lote.coletadoEm)}</dd>
          <dt className="text-gray-500">Fonte</dt>
          <dd className="text-gray-900 font-medium">{FONTE_LABEL[lote.fonte] ?? lote.fonte}</dd>
        </dl>
      </div>
    </div>
  );
}
