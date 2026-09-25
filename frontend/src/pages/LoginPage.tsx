import { FormEvent, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { LockKeyhole, LogIn, UserPlus } from "lucide-react";
import { apiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";

export function LoginPage() {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [fullName, setFullName] = useState("FinVault Admin");
  const [email, setEmail] = useState("admin@finvault.local");
  const [password, setPassword] = useState("FinVault#2026");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const auth = useAuth();
  const location = useLocation();

  if (auth.accessToken) {
    return <Navigate to={(location.state as { from?: Location })?.from?.pathname || "/"} replace />;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (mode === "register") {
        await auth.register(fullName, email, password);
      } else {
        await auth.login(email, password);
      }
    } catch (submitError) {
      setError(apiErrorMessage(submitError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-panel">
        <div className="auth-brand">
          <div className="brand-mark">FV</div>
          <div>
            <h1>FinVault</h1>
            <p>Real-time personal finance intelligence</p>
          </div>
        </div>
        <div className="segmented">
          <button className={mode === "login" ? "selected" : ""} onClick={() => setMode("login")} type="button">
            <LogIn size={16} /> Login
          </button>
          <button className={mode === "register" ? "selected" : ""} onClick={() => setMode("register")} type="button">
            <UserPlus size={16} /> Register
          </button>
        </div>
        <form onSubmit={submit} className="form">
          {mode === "register" && (
            <label>
              Full name
              <input value={fullName} onChange={event => setFullName(event.target.value)} required />
            </label>
          )}
          <label>
            Email
            <input type="email" value={email} onChange={event => setEmail(event.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={event => setPassword(event.target.value)} required />
          </label>
          {error && <div className="error-box">{error}</div>}
          <button className="primary-action" disabled={busy} type="submit">
            <LockKeyhole size={18} />
            {busy ? "Working" : mode === "register" ? "Create account" : "Enter vault"}
          </button>
        </form>
      </section>
    </main>
  );
}
