"use client";

import { useState } from "react";
import { Search, SlidersHorizontal, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { TIPO_LABEL, type FiltrosBusca, type TipoLote } from "@/types";

const UFS = [
  "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA",
  "MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN",
  "RS","RO","RR","SC","SP","SE","TO",
];

interface Props {
  value:    FiltrosBusca;
  onChange: (filtros: FiltrosBusca) => void;
  loading?: boolean;
}

export function FiltrosBuscaPanel({ value, onChange, loading }: Props) {
  const [aberto, setAberto] = useState(false);

  const temFiltros = Object.values(value).some((v) => v != null && v !== "");

  function limpar() {
    onChange({});
  }

  return (
    <div className="card p-4 space-y-4">
      {/* Linha principal: busca por cidade + botão filtros */}
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            className="input pl-9"
            placeholder="Buscar por cidade..."
            value={value.cidade ?? ""}
            onChange={(e) => onChange({ ...value, cidade: e.target.value || undefined })}
          />
        </div>
        <button
          className={cn("btn-ghost border border-gray-200 gap-1.5", aberto && "bg-brand-50 border-brand-200 text-brand-600")}
          onClick={() => setAberto(!aberto)}
        >
          <SlidersHorizontal className="w-4 h-4" />
          Filtros
          {temFiltros && (
            <span className="w-1.5 h-1.5 rounded-full bg-brand-500" />
          )}
        </button>
        {temFiltros && (
          <button className="btn-ghost text-gray-400 hover:text-gray-600 px-2" onClick={limpar}>
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Painel expandido */}
      {aberto && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 pt-2 border-t border-gray-100">

          {/* Estado */}
          <div>
            <label className="text-xs text-gray-500 mb-1 block">Estado</label>
            <select
              className="input"
              value={value.estado ?? ""}
              onChange={(e) => onChange({ ...value, estado: e.target.value || undefined })}
            >
              <option value="">Todos</option>
              {UFS.map((uf) => <option key={uf} value={uf}>{uf}</option>)}
            </select>
          </div>

          {/* Tipo */}
          <div>
            <label className="text-xs text-gray-500 mb-1 block">Tipo</label>
            <select
              className="input"
              value={value.tipo ?? ""}
              onChange={(e) => onChange({ ...value, tipo: (e.target.value as TipoLote) || undefined })}
            >
              <option value="">Todos</option>
              {Object.entries(TIPO_LABEL).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
          </div>

          {/* Valor máximo */}
          <div>
            <label className="text-xs text-gray-500 mb-1 block">Valor máximo (R$)</label>
            <input
              type="number"
              className="input"
              placeholder="ex: 500000"
              value={value.valorMaximo ?? ""}
              onChange={(e) => onChange({ ...value, valorMaximo: e.target.value ? Number(e.target.value) : undefined })}
            />
          </div>

          {/* Score mínimo */}
          <div>
            <label className="text-xs text-gray-500 mb-1 block">
              Score mínimo: {value.scoreMinimo ?? 0}
            </label>
            <input
              type="range"
              min={0} max={100} step={5}
              className="w-full accent-brand-500"
              value={value.scoreMinimo ?? 0}
              onChange={(e) => onChange({ ...value, scoreMinimo: Number(e.target.value) || undefined })}
            />
          </div>

          {/* Checkboxes */}
          <div className="flex flex-col gap-2">
            <label className="text-xs text-gray-500">Financiamento</label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                className="accent-brand-500"
                checked={value.aceitaFinanciamento ?? false}
                onChange={(e) => onChange({ ...value, aceitaFinanciamento: e.target.checked || undefined })}
              />
              Aceita financiamento
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input
                type="checkbox"
                className="accent-brand-500"
                checked={value.aceitaFgts ?? false}
                onChange={(e) => onChange({ ...value, aceitaFgts: e.target.checked || undefined })}
              />
              Aceita FGTS
            </label>
          </div>
        </div>
      )}
    </div>
  );
}
