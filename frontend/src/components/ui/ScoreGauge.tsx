"use client";

import { cn } from "@/lib/utils";

interface Props {
  score: number | null;
  size?: "sm" | "md" | "lg";
  showLabel?: boolean;
}

const FAIXAS = [
  { min: 80, label: "Excelente",  cor: "#10b981" },
  { min: 65, label: "Muito bom",  cor: "#3b82f6" },
  { min: 50, label: "Bom",        cor: "#0ea5e9" },
  { min: 35, label: "Regular",    cor: "#f59e0b" },
  { min: 0,  label: "Baixo",      cor: "#9ca3af" },
];

function getFaixa(score: number) {
  return FAIXAS.find((f) => score >= f.min) ?? FAIXAS[FAIXAS.length - 1];
}

export function ScoreGauge({ score, size = "md", showLabel = true }: Props) {
  if (score == null) {
    return (
      <div className="flex flex-col items-center gap-1">
        <div className={cn(
          "rounded-full border-4 border-gray-100 flex items-center justify-center",
          size === "lg" ? "w-24 h-24" : size === "md" ? "w-16 h-16" : "w-12 h-12"
        )}>
          <span className={cn(
            "font-semibold text-gray-300",
            size === "lg" ? "text-2xl" : size === "md" ? "text-base" : "text-sm"
          )}>—</span>
        </div>
        {showLabel && <span className="text-xs text-gray-400">Sem score</span>}
      </div>
    );
  }

  const faixa    = getFaixa(score);
  const circunf  = 2 * Math.PI * 38; // raio 38
  const progresso = circunf * (score / 100);

  const dim = size === "lg" ? 96 : size === "md" ? 64 : 48;
  const r   = size === "lg" ? 38 : size === "md" ? 24 : 18;
  const sw  = size === "lg" ? 6  : size === "md" ? 5  : 4;
  const c   = dim / 2;

  return (
    <div className="flex flex-col items-center gap-1">
      <div style={{ width: dim, height: dim }} className="relative">
        <svg width={dim} height={dim} viewBox={`0 0 ${dim} ${dim}`}>
          {/* Track */}
          <circle
            cx={c} cy={c} r={r}
            fill="none"
            stroke="#f3f4f6"
            strokeWidth={sw}
          />
          {/* Progresso */}
          <circle
            cx={c} cy={c} r={r}
            fill="none"
            stroke={faixa.cor}
            strokeWidth={sw}
            strokeDasharray={`${2 * Math.PI * r}`}
            strokeDashoffset={`${2 * Math.PI * r * (1 - score / 100)}`}
            strokeLinecap="round"
            transform={`rotate(-90 ${c} ${c})`}
          />
        </svg>
        {/* Número central */}
        <div className="absolute inset-0 flex items-center justify-center">
          <span
            className={cn(
              "font-bold tabular-nums",
              size === "lg" ? "text-2xl" : size === "md" ? "text-base" : "text-sm"
            )}
            style={{ color: faixa.cor }}
          >
            {score.toFixed(0)}
          </span>
        </div>
      </div>
      {showLabel && (
        <span className="text-xs font-medium" style={{ color: faixa.cor }}>
          {faixa.label}
        </span>
      )}
    </div>
  );
}
