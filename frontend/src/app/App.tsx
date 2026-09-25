import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "../routes/ProtectedRoute";
import { AppLayout } from "../layouts/AppLayout";
import { AdminPage } from "../pages/AdminPage";
import { AnalyticsPage } from "../pages/AnalyticsPage";
import { BudgetsPage } from "../pages/BudgetsPage";
import { DashboardPage } from "../pages/DashboardPage";
import { GoalsPage } from "../pages/GoalsPage";
import { LoginPage } from "../pages/LoginPage";
import { ReportsPage } from "../pages/ReportsPage";
import { TransactionsPage } from "../pages/TransactionsPage";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/budgets" element={<BudgetsPage />} />
          <Route path="/goals" element={<GoalsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
        </Route>
      </Route>
      <Route element={<ProtectedRoute roles={["SUPER_ADMIN"]} />}>
        <Route element={<AppLayout />}>
          <Route path="/admin" element={<AdminPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
