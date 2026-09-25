export function ProgressBar({ value, dangerAt = 100 }: { value: number; dangerAt?: number }) {
  const clamped = Math.max(0, Math.min(100, value));
  const tone = value >= dangerAt ? "danger" : value >= 80 ? "warning" : "normal";
  return (
    <div className={`progress ${tone}`} aria-label={`${clamped.toFixed(0)} percent`}>
      <span style={{ width: `${clamped}%` }} />
    </div>
  );
}
