import { ChangeEvent, FormEvent, useEffect, useState } from "react";
import { Upload, Plus, Trash2 } from "lucide-react";
import { api, apiErrorMessage, unwrap } from "../api/client";
import type { ApiResponse, BulkImportResponse, TransactionRequest, TransactionResponse, TransactionType } from "../api/types";
import { money, todayDateTimeLocal } from "../utils/format";

const types: TransactionType[] = ["EXPENSE", "INCOME", "SAVINGS", "TRANSFER"];

export function TransactionsPage() {
  const [items, setItems] = useState<TransactionResponse[]>([]);
  const [form, setForm] = useState<TransactionRequest>({
    type: "EXPENSE",
    category: "Groceries",
    merchant: "Fresh Market",
    amount: 75,
    occurredAt: todayDateTimeLocal(),
    description: "Weekly food run"
  });
  const [error, setError] = useState("");
  const [importResult, setImportResult] = useState<BulkImportResponse | null>(null);

  const load = () => api.get<ApiResponse<TransactionResponse[]>>("/transactions")
    .then(response => setItems(unwrap(response)))
    .catch(loadError => setError(apiErrorMessage(loadError)));

  useEffect(() => {
    load();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const response = await api.post<ApiResponse<TransactionResponse>>("/transactions", form, {
        headers: { "Idempotency-Key": crypto.randomUUID() }
      });
      setItems(current => [unwrap(response), ...current]);
    } catch (submitError) {
      setError(apiErrorMessage(submitError));
    }
  }

  async function deleteItem(id: number) {
    await api.delete(`/transactions/${id}`);
    setItems(current => current.filter(item => item.id !== id));
  }

  async function importCsv(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    const body = new FormData();
    body.append("file", file);
    try {
      const response = await api.post<ApiResponse<BulkImportResponse>>("/transactions/import", body, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      setImportResult(unwrap(response));
      load();
    } catch (importError) {
      setError(apiErrorMessage(importError));
    }
  }

  return (
    <div className="stack">
      <section className="panel">
        <h2>New Transaction</h2>
        <form className="form grid-form" onSubmit={submit}>
          <label>
            Type
            <select value={form.type} onChange={event => setForm({ ...form, type: event.target.value as TransactionType })}>
              {types.map(type => <option key={type}>{type}</option>)}
            </select>
          </label>
          <label>
            Category
            <input value={form.category} onChange={event => setForm({ ...form, category: event.target.value })} required />
          </label>
          <label>
            Merchant
            <input value={form.merchant} onChange={event => setForm({ ...form, merchant: event.target.value })} required />
          </label>
          <label>
            Amount
            <input type="number" min="1" step="0.01" value={form.amount} onChange={event => setForm({ ...form, amount: Number(event.target.value) })} required />
          </label>
          <label>
            Occurred
            <input type="datetime-local" value={form.occurredAt} onChange={event => setForm({ ...form, occurredAt: event.target.value })} required />
          </label>
          <label>
            Description
            <input value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} />
          </label>
          <button className="primary-action" type="submit"><Plus size={18} /> Save transaction</button>
          <label className="file-button">
            <Upload size={18} />
            CSV import
            <input type="file" accept=".csv,text/csv" onChange={importCsv} />
          </label>
        </form>
        {error && <div className="error-box">{error}</div>}
        {importResult && (
          <div className="success-box">
            Imported {importResult.successRows} of {importResult.totalRows} rows. Failed rows: {importResult.failedRows}.
          </div>
        )}
      </section>
      <section className="panel">
        <h2>Ledger</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Merchant</th>
                <th>Category</th>
                <th>Amount</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.id}>
                  <td>{item.occurredAt?.slice(0, 10)}</td>
                  <td>{item.type}</td>
                  <td>{item.merchant}</td>
                  <td>{item.category}</td>
                  <td>{money(item.amount)}</td>
                  <td>
                    <button className="icon-only" title="Delete transaction" onClick={() => deleteItem(item.id)}>
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
