import { useEffect, useState } from "react";
import { Activity, LineChart, Store, TrendingUp } from "lucide-react";
import { api, apiErrorMessage, unwrap } from "../api/client";
import type { AnalyticsOverview, ApiResponse } from "../api/types";
import { MerchantBar, TrendLine } from "../charts/SpendingCharts";
import { StatTile } from "../components/StatTile";
import { money, percent } from "../utils/format";

export function AnalyticsPage() {
  const [analytics, setAnalytics] = useState<AnalyticsOverview | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<ApiResponse<AnalyticsOverview>>("/analytics/overview")
      .then(response => setAnalytics(unwrap(response)))
      .catch(loadError => setError(apiErrorMessage(loadError)));
  }, []);

  if (error) {
    return <div className="error-box">{error}</div>;
  }
  if (!analytics) {
    return <div className="muted">Loading analytics.</div>;
  }

  return (
    <div className="stack">
      <div className="stats-grid">
        <StatTile label="Prediction" value={money(analytics.nextMonthPrediction)} icon={LineChart} tone="blue" />
        <StatTile label="Growth" value={percent(analytics.monthOverMonthGrowth)} icon={TrendingUp} tone="green" />
        <StatTile label="Merchants" value={String(analytics.topMerchants.length)} icon={Store} tone="gold" />
        <StatTile label="Heatmap cells" value={String(analytics.heatmap.length)} icon={Activity} tone="red" />
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel">
          <h2>Monthly Spending</h2>
          <TrendLine analytics={analytics} />
        </section>
        <section className="panel chart-panel">
          <h2>Top Merchants</h2>
          <MerchantBar analytics={analytics} />
        </section>
      </div>
      <section className="panel">
        <h2>Heatmap</h2>
        <div className="heatmap">
          {analytics.heatmap.map(cell => (
            <span key={`${cell.category}-${cell.day}`} title={`${cell.category} day ${cell.day}: ${money(cell.total)}`}>
              {cell.day}
            </span>
          ))}
        </div>
      </section>
    </div>
  );
}
