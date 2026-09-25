import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip
} from "chart.js";
import { Bar, Doughnut, Line } from "react-chartjs-2";
import type { AnalyticsOverview } from "../api/types";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, ArcElement, Tooltip, Legend);

const palette = ["#1f7a8c", "#84a98c", "#f2b84b", "#c8553d", "#6d597a", "#2f4858"];

export function TrendLine({ analytics }: { analytics: AnalyticsOverview }) {
  return (
    <Line
      data={{
        labels: analytics.monthlyTrends.map(item => item.month),
        datasets: [{
          label: "Monthly spend",
          data: analytics.monthlyTrends.map(item => Number(item.total)),
          borderColor: "#1f7a8c",
          backgroundColor: "rgba(31, 122, 140, 0.18)",
          tension: 0.35,
          fill: true
        }]
      }}
      options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }}
    />
  );
}

export function MerchantBar({ analytics }: { analytics: AnalyticsOverview }) {
  return (
    <Bar
      data={{
        labels: analytics.topMerchants.map(item => item.merchant),
        datasets: [{
          label: "Merchant spend",
          data: analytics.topMerchants.map(item => Number(item.total)),
          backgroundColor: "#84a98c"
        }]
      }}
      options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }}
    />
  );
}

export function CategoryDoughnut({ analytics }: { analytics: AnalyticsOverview }) {
  const entries = Object.entries(analytics.categoryTotals);
  return (
    <Doughnut
      data={{
        labels: entries.map(([label]) => label),
        datasets: [{
          data: entries.map(([, value]) => Number(value)),
          backgroundColor: palette
        }]
      }}
      options={{ responsive: true, maintainAspectRatio: false, plugins: { legend: { position: "bottom" } } }}
    />
  );
}
