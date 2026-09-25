import { BarChart3, Bell, FileText, Flag, LayoutDashboard, LogOut, ReceiptText, Shield, WalletCards } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";
import { useLiveFeed } from "../hooks/useLiveFeed";
import { useAppState } from "../context/AppStateContext";
import { useAuth } from "../context/AuthContext";

const nav = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/transactions", label: "Transactions", icon: ReceiptText },
  { to: "/analytics", label: "Analytics", icon: BarChart3 },
  { to: "/budgets", label: "Budgets", icon: WalletCards },
  { to: "/goals", label: "Goals", icon: Flag },
  { to: "/reports", label: "Reports", icon: FileText }
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const { state } = useAppState();
  useLiveFeed(user?.id);

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">FV</div>
          <div>
            <strong>FinVault</strong>
            <span>Finance Intelligence</span>
          </div>
        </div>
        <nav className="nav">
          {nav.map(item => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => isActive ? "active" : undefined} end={item.to === "/"}>
              <item.icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}
          {user?.role === "SUPER_ADMIN" && (
            <NavLink to="/admin" className={({ isActive }) => isActive ? "active" : undefined}>
              <Shield size={18} />
              <span>Admin</span>
            </NavLink>
          )}
        </nav>
        <button className="icon-text ghost" onClick={logout} title="Log out">
          <LogOut size={18} />
          <span>Log out</span>
        </button>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Signed in as {user?.role}</p>
            <h1>{user?.fullName || user?.email}</h1>
          </div>
          <div className="live-chip">
            <Bell size={16} />
            <span>{state.feed.length} live updates</span>
          </div>
        </header>
        <div className="content-grid">
          <section className="page-surface">
            <Outlet />
          </section>
          <aside className="feed-panel">
            <h2>Live Feed</h2>
            {state.feed.length === 0 ? (
              <p className="muted">Waiting for account activity.</p>
            ) : state.feed.map(item => (
              <article key={item.id} className={`feed-item ${item.kind}`}>
                <strong>{item.title}</strong>
                <span>{item.detail}</span>
              </article>
            ))}
          </aside>
        </div>
      </main>
    </div>
  );
}
