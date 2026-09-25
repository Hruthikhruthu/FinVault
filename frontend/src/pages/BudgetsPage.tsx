import { FormEvent, useEffect, useState } from "react";
import { Check, Plus } from "lucide-react";
import { api, apiErrorMessage, unwrap } from "../api/client";
import type { AlertResponse, ApiResponse, BudgetRequest, BudgetResponse } from "../api/types";
import { ProgressBar } from "../components/ProgressBar";
import { money, percent, todayMonthKey } from "../utils/format";

export function BudgetsPage() {
  const [budgets, setBudgets] = useState<BudgetResponse[]>([]);
  const [alerts, setAlerts] = useState<AlertResponse[]>([]);
  const [form, setForm] = useState<BudgetRequest>({ monthKey: todayMonthKey(), category: "Groceries", limitAmount: 250 });
  const [error, setError] = useState("");

  const load = () => Promise.all([
    api.get<ApiResponse<BudgetResponse[]>>("/budgets"),
    api.get<ApiResponse<AlertResponse[]>>("/budgets/alerts")
  ]).then(([budgetResponse, alertResponse]) => {
    setBudgets(unwrap(budgetResponse));
    setAlerts(unwrap(alertResponse));
  }).catch(loadError => setError(apiErrorMessage(loadError)));

  useEffect(() => {
    load();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      await api.post<ApiResponse<BudgetResponse>>("/budgets", form);
      load();
    } catch (submitError) {
      setError(apiErrorMessage(submitError));
    }
  }

  async function markRead(id: number) {
    await api.patch(`/budgets/alerts/${id}/read`);
    setAlerts(current => current.map(alert => alert.id === id ? { ...alert, read: true } : alert));
  }

  return (
    <div className="stack">
      <section className="panel">
        <h2>Monthly Budget</h2>
        <form className="form grid-form" onSubmit={submit}>
          <label>
            Month
            <input value={form.monthKey} onChange={event => setForm({ ...form, monthKey: event.target.value })} pattern="\d{4}-\d{2}" required />
          </label>
          <label>
            Category
            <input value={form.category} onChange={event => setForm({ ...form, category: event.target.value })} required />
          </label>
          <label>
            Limit
            <input type="number" min="1" step="0.01" value={form.limitAmount} onChange={event => setForm({ ...form, limitAmount: Number(event.target.value) })} required />
          </label>
          <button className="primary-action" type="submit"><Plus size={18} /> Save budget</button>
        </form>
        {error && <div className="error-box">{error}</div>}
      </section>
      <section className="panel">
        <h2>Budgets</h2>
        <div className="list">
          {budgets.map(budget => (
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
        </div>
      </section>
      <section className="panel">
        <h2>Alerts</h2>
        <div className="list">
          {alerts.map(alert => (
            <article className={`list-row severity-${alert.severity.toLowerCase()}`} key={alert.id}>
              <div>
                <strong>{alert.severity}</strong>
                <span>{alert.message}</span>
              </div>
              {!alert.read && (
                <button className="icon-only" onClick={() => markRead(alert.id)} title="Mark read">
                  <Check size={16} />
                </button>
              )}
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
