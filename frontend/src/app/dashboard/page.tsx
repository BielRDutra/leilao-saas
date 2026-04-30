"use client";

import useSWR from "swr";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip,
  PieChart, Pie, Cell, ResponsiveContainer, Legend,
} from "recharts";
import {
  Database, TrendingUp, Clock, Award,
  RefreshCw, LayoutDashboard,
} from "lucide-react";
import { getResumo, formatarData, recalcularScore } from "@/lib/api";
import { FONTE_LABEL, TIPO_LABEL } from "@/types";
import { useState } from "react";

const CORES = ["#534AB7", "#10b981", "#3b82f6", "#f59e0b", "#ef4444", "#8b5cf6"];

function MetricaCard({
  icone: Icon, label, valor, sub, cor = "text-brand-600",
}: {
  icone: React.ElementType;
  label: string;
  valor: string | number;
  sub?: string;
  cor?: string;
}) {
  return (
    <div className="card p-4">
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-lg bg-gray-50 ${cor}`}>
          <Icon className="w-5 h-5" />
        </div>
        <div>
          <p className="text-xs text-gray-500">{label}</p>
          <p className="text-xl font-bold text-gray-900 tabular-nums">{valor}</p>
          {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
        </div>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { data, isLoading, mutate } = useSWR("resumo", getResumo, {
    refreshInterval: 60_000,
  });
  const [recalculando, setRecalculando] = useState(false);

  async function handleRecalcular() {
    setRecalculando(true);
    try {
      const res = await recalcularScore();
      alert(`Score recalculado para ${res.lotesPontuados} lotes.`);
      mutate();
    } catch {
      alert("Erro ao recalcular score.");
    } finally {
      setRecalculando(false);
    }
  }

  // Prepara dados dos gráficos
  const dadosFonte = Object.entries(data?.porFonte ?? {}).map(([key, val]) => ({
    name: FONTE_LABEL[key] ?? key,
    lotes: val,
  }));

  const dadosTipo = Object.entries(data?.porTipo ?? {})
    .filter(([, v]) => v > 0)
    .map(([key, val]) => ({
      name: TIPO_LABEL[key as keyof typeof TIPO_LABEL] ?? key,
      value: val,
    }));

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <LayoutDashboard className="w-5 h-5 text-brand-500" />
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        </div>
        <div className="flex items-center gap-2">
          <button
            className="btn-ghost text-sm"
            onClick={handleRecalcular}
            disabled={recalculando}
          >
            <RefreshCw className={`w-4 h-4 ${recalculando ? "animate-spin" : ""}`} />
            Recalcular scores
          </button>
          <button className="btn-ghost text-sm" onClick={() => mutate()}>
            <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
            Atualizar
          </button>
        </div>
      </div>

      {/* Métricas */}
      {isLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card p-4 h-20 animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricaCard
            icone={Database}
            label="Total de lotes"
            valor={(data?.totalLotes ?? 0).toLocaleString("pt-BR")}
            sub={`${data?.lotesDisponiveis ?? 0} disponíveis`}
            cor="text-brand-600"
          />
          <MetricaCard
            icone={Award}
            label="Com score"
            valor={(data?.lotesComScore ?? 0).toLocaleString("pt-BR")}
            sub={`de ${data?.totalLotes ?? 0} total`}
            cor="text-emerald-600"
          />
          <MetricaCard
            icone={TrendingUp}
            label="Score mediano"
            valor={data?.scoreMediano?.toFixed(1) ?? "—"}
            sub="oportunidade média"
            cor="text-blue-600"
          />
          <MetricaCard
            icone={Clock}
            label="Coletados hoje"
            valor={(data?.coletadosHoje ?? 0).toLocaleString("pt-BR")}
            sub={data?.ultimaColeta ? `Última: ${formatarData(data.ultimaColeta)}` : "Sem coleta"}
            cor="text-amber-600"
          />
        </div>
      )}

      {/* Gráficos */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

        {/* Por fonte */}
        <div className="card p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Lotes por portal</h2>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={dadosFonte} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                formatter={(v: number) => [v.toLocaleString("pt-BR"), "lotes"]}
                contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid #e5e7eb" }}
              />
              <Bar dataKey="lotes" fill="#534AB7" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Por tipo */}
        <div className="card p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Lotes por tipo</h2>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie
                data={dadosTipo}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={80}
                label={({ name, percent }) =>
                  `${name.split(" ")[0]} ${(percent * 100).toFixed(0)}%`
                }
                labelLine={false}
              >
                {dadosTipo.map((_, i) => (
                  <Cell key={i} fill={CORES[i % CORES.length]} />
                ))}
              </Pie>
              <Tooltip
                formatter={(v: number) => [v.toLocaleString("pt-BR"), "lotes"]}
                contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid #e5e7eb" }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Última coleta */}
      {data?.ultimaColeta && (
        <p className="text-xs text-gray-400 text-center">
          Dados atualizados em {formatarData(data.ultimaColeta)}
        </p>
      )}
    </div>
  );
}
