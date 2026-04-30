"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Gavel, Search, BarChart2, LayoutDashboard } from "lucide-react";
import { cn } from "@/lib/utils";

const links = [
  { href: "/ranking",   label: "Ranking",    icon: Gavel },
  { href: "/buscar",    label: "Buscar",      icon: Search },
  { href: "/dashboard", label: "Dashboard",  icon: LayoutDashboard },
];

export function Navbar() {
  const pathname = usePathname();

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-gray-100 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">

          {/* Logo */}
          <Link href="/" className="flex items-center gap-2 font-semibold text-brand-600">
            <Gavel className="w-5 h-5" />
            <span>LeilãoSmart</span>
          </Link>

          {/* Nav links */}
          <nav className="flex items-center gap-1">
            {links.map(({ href, label, icon: Icon }) => (
              <Link
                key={href}
                href={href}
                className={cn(
                  "flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                  pathname === href
                    ? "bg-brand-50 text-brand-600"
                    : "text-gray-600 hover:bg-gray-100"
                )}
              >
                <Icon className="w-4 h-4" />
                {label}
              </Link>
            ))}
          </nav>
        </div>
      </div>
    </header>
  );
}
