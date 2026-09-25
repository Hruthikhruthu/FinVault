import { useEffect, useMemo, useState } from "react";
import { AlertTriangle, PiggyBank, ReceiptText, TrendingUp } from "lucide-react";
import { api, apiErrorMessage, unwrap } from "../api/client";
import type { AlertResponse, AnalyticsOverview, ApiResponse, BudgetResponse, GoalResponse, TransactionResponse } from "../api/types";
import { StatTile } from "../components/StatTile";
import { ProgressBar } from "../components/ProgressBar";
import { CategoryDoughnut, TrendLine } from "../charts/SpendingCharts";
import { money, percent } from "../utils/format";

export function DashboardPage() {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [analytics, setAnalytics] = useState<AnalyticsOverview | null>(null);
  const [budgets, setBudgets] = useState<BudgetResponse[]>([]);
  const [alerts, setAlerts] = useState<AlertResponse[]>([]);
  const [goals, setGoals] = useState<GoalResponse[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([
      api.get<ApiResponse<TransactionResponse[]>>("/transactions"),
      api.get<ApiResponse<AnalyticsOverview>>("/analytics/overview"),
      api.get<ApiResponse<BudgetResponse[]>>("/budgets"),
      api.get<ApiResponse<AlertResponse[]>>("/budgets/alerts"),
      api.get<ApiResponse<GoalResponse[]>>("/goals")
    ])
      .then(([tx, overview, budget, alert, goal]) => {
        setTransactions(unwrap(tx));
        setAnalytics(unwrap(overview));
        setBudgets(unwrap(budget));
        setAlerts(unwrap(alert));
        setGoals(unwrap(goal));
      })
      .catch(loadError => setError(apiErrorMessage(loadError)));
  }, []);

  const totals = useMemo(() => {
    const expense = transactions.filter(item => item.type === "EXPENSE" || item.type === "SAVINGS")
      .reduce((sum, item) => sum + Number(item.amount), 0);
    const income = transactions.filter(item => item.type === "INCOME")
      .reduce((sum, item) => sum + Number(item.amount), 0);
    return { expense, income };
  }, [transactions]);

  return (
    <div className="stack">
      {error && <div className="error-box">{error}</div>}
      <div className="stats-grid">
        <StatTile label="Income" value={money(totals.income)} icon={TrendingUp} tone="green" />
        <StatTile label="Spend" value={money(totals.expense)} icon={ReceiptText} tone="blue" />
        <StatTile label="Unread alerts" value={String(alerts.filter(alert => !alert.read).length)} icon={AlertTriangle} tone="red" />
        <StatTile label="Open goals" value={String(goals.filter(goal => !goal.completed).length)} icon={PiggyBank} tone="gold" />
      </div>
      {analytics && (
        <div className="dashboard-grid">
          <section className="panel chart-panel">
            <h2>Spending Trend</h2>
            <TrendLine analytics={analytics} />
          </section>
          <section className="panel chart-panel">
            <h2>Category Mix</h2>
            <CategoryDoughnut analytics={analytics} />
          </section>
        </div>
      )}
      <section className="panel">
        <h2>Budget Position</h2>
        <div className="list">
          {budgets.slice(0, 5).map(budget => (
            <article className="list-row" key={budget.id}>
              <div>
                <strong>{budget.category}</strong>
                <span>{budget.monthKey} - {money(budget.spentAmount)} of {money(budget.limitAmount)}</span>
              </div>
              <div className="row-progress">
                <ProgressBar value={budget.percentUsed} />
                <small>{percent(budget.percentUsed)}</small>
              </div>
            </article>
          ))}
          {budgets.length === 0 && <p className="muted">No budgets yet.</p>}
        </div>
      </section>
    </div>
  );
}
