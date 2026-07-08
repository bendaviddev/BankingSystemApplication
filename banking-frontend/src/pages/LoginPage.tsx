import { type FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";

export function LoginPage() {
  const { session, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (session) {
    return <Navigate to="/app" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await api.login({ username, password });
      login({
        token: res.token,
        userId: res.userId,
        username: res.username,
        role: res.role,
        firstName: res.firstName,
      });
      navigate("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <Link to="/" className="auth-brand">
          <div className="auth-brand-icon">🏦</div>
          <span className="auth-brand-name">Ben Banking</span>
        </Link>

        <h1 className="auth-heading">Welcome back</h1>
        <p className="auth-subtitle">Sign in to access your accounts</p>

        {error && <div className="alert error" role="alert">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <Input
            id="username"
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoComplete="username"
            placeholder="Enter your username"
            autoFocus
          />

          <Input
            id="password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
            placeholder="••••••••"
          />

          <Button type="submit" fullWidth loading={loading}>
            Sign in
          </Button>
        </form>

        <div className="demo-hint">
          Demo credentials: <code>demo</code> / <code>Demo123!</code>
        </div>

        <div className="auth-footer">
          New here? <Link to="/register">Create an account</Link>
        </div>
      </div>
    </div>
  );
}
