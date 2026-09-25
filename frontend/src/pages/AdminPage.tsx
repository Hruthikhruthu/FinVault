import { useEffect, useState } from "react";
import { KeyRound, Power, RefreshCcw } from "lucide-react";
import { api, apiErrorMessage, unwrap } from "../api/client";
import type { AdminUserResponse, ApiResponse, AuditResponse, MetricsResponse } from "../api/types";

export function AdminPage() {
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [audit, setAudit] = useState<AuditResponse[]>([]);
  const [metrics, setMetrics] = useState<MetricsResponse | null>(null);
  const [error, setError] = useState("");

  const load = () => Promise.all([
    api.get<ApiResponse<AdminUserResponse[]>>("/admin/users"),
    api.get<ApiResponse<AuditResponse[]>>("/admin/audit"),
    api.get<ApiResponse<MetricsResponse>>("/admin/metrics")
  ]).then(([userResponse, auditResponse, metricsResponse]) => {
    setUsers(unwrap(userResponse));
    setAudit(unwrap(auditResponse));
    setMetrics(unwrap(metricsResponse));
  }).catch(loadError => setError(apiErrorMessage(loadError)));

  useEffect(() => {
    load();
  }, []);

  async function resetPassword(id: number) {
    const newPassword = window.prompt("New password", "FinVault#2026");
    if (!newPassword) {
      return;
    }
    await api.post(`/admin/users/${id}/reset-password`, { newPassword });
    load();
  }

  async function deactivate(id: number) {
    await api.patch(`/admin/users/${id}/deactivate`);
    load();
  }

  return (
    <div className="stack">
      <section className="panel">
        <div className="panel-title">
          <h2>Metrics</h2>
          <button className="icon-only" onClick={load} title="Refresh metrics"><RefreshCcw size={16} /></button>
        </div>
        {metrics && (
          <div className="metrics-grid">
            <code>users: {metrics.users}</code>
            <code>alerts: {metrics.unreadAlerts}</code>
            <code>ws: {metrics.activeWebSocketSessions}</code>
            <code>redis: {String(metrics.redis.status)}</code>
            <code>hikari: {JSON.stringify(metrics.hikari)}</code>
          </div>
        )}
        {error && <div className="error-box">{error}</div>}
      </section>
      <section className="panel">
        <h2>Users</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Failed</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td>{user.email}</td>
                  <td>{user.role}</td>
                  <td>{user.enabled ? "Active" : "Inactive"}</td>
                  <td>{user.failedAttempts}</td>
                  <td className="button-cell">
                    <button className="icon-only" onClick={() => resetPassword(user.id)} title="Reset password"><KeyRound size={16} /></button>
                    <button className="icon-only danger-button" onClick={() => deactivate(user.id)} title="Deactivate account"><Power size={16} /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section className="panel">
        <h2>Audit Log</h2>
        <div className="list">
          {audit.map(entry => (
            <article className="list-row" key={entry.id}>
              <div>
                <strong>{entry.action} - {entry.status}</strong>
                <span>{entry.actorEmail || "system"} on {entry.target || "system"}</span>
                <span>{entry.details}</span>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
