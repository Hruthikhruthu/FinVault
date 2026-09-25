import { FileSpreadsheet, FileText } from "lucide-react";
import { api, apiErrorMessage } from "../api/client";
import { useState } from "react";

export function ReportsPage() {
  const [error, setError] = useState("");

  async function download(path: string, fileName: string) {
    try {
      const response = await api.get(path, { responseType: "blob" });
      const url = URL.createObjectURL(response.data);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (downloadError) {
      setError(apiErrorMessage(downloadError));
    }
  }

  return (
    <div className="stack">
      <section className="panel report-actions">
        <h2>Reports</h2>
        <button className="primary-action" onClick={() => download("/reports/pdf", "finvault-report.pdf")}>
          <FileText size={18} /> Download PDF
        </button>
        <button className="secondary-action" onClick={() => download("/reports/excel", "finvault-report.xlsx")}>
          <FileSpreadsheet size={18} /> Download Excel
        </button>
        {error && <div className="error-box">{error}</div>}
      </section>
    </div>
  );
}
