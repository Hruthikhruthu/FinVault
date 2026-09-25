import { FormEvent, useEffect, useState } from "react";
import { Plus } from "lucide-react";
import { api, apiErrorMessage, unwrap } from "../api/client";
import type { ApiResponse, GoalRequest, GoalResponse } from "../api/types";
import { ProgressBar } from "../components/ProgressBar";
import { futureDate, money, percent } from "../utils/format";

export function GoalsPage() {
  const [goals, setGoals] = useState<GoalResponse[]>([]);
  const [form, setForm] = useState<GoalRequest>({
    name: "Emergency Fund",
    category: "Savings",
    targetAmount: 5000,
    currentAmount: 250,
    deadline: futureDate(180)
  });
  const [error, setError] = useState("");

  const load = () => api.get<ApiResponse<GoalResponse[]>>("/goals")
    .then(response => setGoals(unwrap(response)))
    .catch(loadError => setError(apiErrorMessage(loadError)));

  useEffect(() => {
    load();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    try {
      await api.post<ApiResponse<GoalResponse>>("/goals", form);
      load();
    } catch (submitError) {
      setError(apiErrorMessage(submitError));
    }
  }

  return (
    <div className="stack">
      <section className="panel">
        <h2>Savings Goal</h2>
        <form className="form grid-form" onSubmit={submit}>
          <label>
            Name
            <input value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} required />
          </label>
          <label>
            Category
            <input value={form.category} onChange={event => setForm({ ...form, category: event.target.value })} required />
          </label>
          <label>
            Target
            <input type="number" min="1" step="0.01" value={form.targetAmount} onChange={event => setForm({ ...form, targetAmount: Number(event.target.value) })} required />
          </label>
          <label>
            Current
            <input type="number" min="0" step="0.01" value={form.currentAmount} onChange={event => setForm({ ...form, currentAmount: Number(event.target.value) })} required />
          </label>
          <label>
            Deadline
            <input type="date" value={form.deadline} onChange={event => setForm({ ...form, deadline: event.target.value })} required />
          </label>
          <button className="primary-action" type="submit"><Plus size={18} /> Save goal</button>
        </form>
        {error && <div className="error-box">{error}</div>}
      </section>
      <section className="panel">
        <h2>Goals</h2>
        <div className="list">
          {goals.map(goal => (
            <article className="list-row goal-row" key={goal.id}>
              <div>
                <strong>{goal.name}</strong>
                <span>{money(goal.currentAmount)} of {money(goal.targetAmount)} by {goal.deadline}</span>
                <span>{money(goal.dailySavingsRequired)} per day, predicted {goal.predictedCompletionDate}</span>
              </div>
              <div className="row-progress">
                <ProgressBar value={goal.progressPercent} />
                <small>{percent(goal.progressPercent)}</small>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
