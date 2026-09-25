import type { LucideIcon } from "lucide-react";

export function StatTile({ label, value, icon: Icon, tone = "blue" }: {
  label: string;
  value: string;
  icon: LucideIcon;
  tone?: "blue" | "green" | "gold" | "red";
}) {
  return (
    <article className={`stat-tile ${tone}`}>
      <div className="stat-icon">
        <Icon size={20} />
      </div>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
    </article>
  );
}
