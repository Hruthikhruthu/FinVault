import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { Role } from "../api/types";

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { accessToken, ready, user } = useAuth();
  const location = useLocation();

  if (!ready) {
    return <div className="center-screen">Loading FinVault</div>;
  }
  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
