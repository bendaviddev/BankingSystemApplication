import { type FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";

export function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: "",
    password: "",
    firstName: "",
    lastName: "",
    phoneNumber: "",
    email: "",
    address: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await api.register(form);
      login({
        token: res.token,
        userId: res.userId,
        username: res.username,
        role: res.role,
      });
      navigate("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="auth-brand-icon">🏦</div>
          <span className="auth-brand-name">Ben Banking</span>
        </div>

        <h1 className="auth-heading">Create account</h1>
        <p className="auth-subtitle">Join Ben Banking in seconds</p>

        {error && <div className="alert error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="field-row">
            <div>
              <label htmlFor="firstName">First name</label>
              <input
                id="firstName"
                value={form.firstName}
                onChange={(e) => update("firstName", e.target.value)}
                required
                placeholder="Jane"
                autoFocus
              />
            </div>
            <div>
              <label htmlFor="lastName">Last name</label>
              <input
                id="lastName"
                value={form.lastName}
                onChange={(e) => update("lastName", e.target.value)}
                required
                placeholder="Smith"
              />
            </div>
          </div>

          <label htmlFor="username">Username</label>
          <input
            id="username"
            value={form.username}
            onChange={(e) => update("username", e.target.value)}
            required
            autoComplete="username"
            placeholder="janesmith"
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={form.password}
            onChange={(e) => update("password", e.target.value)}
            required
            autoComplete="new-password"
            placeholder="••••••••"
          />
          <p className="password-hint">
            8+ chars · uppercase letter · number · special character
          </p>

          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
            autoComplete="email"
            placeholder="jane@example.com"
          />

          <label htmlFor="phone">
            Phone <span style={{ fontWeight: 400, textTransform: "none" }}>(optional)</span>
          </label>
          <input
            id="phone"
            type="tel"
            value={form.phoneNumber}
            onChange={(e) => update("phoneNumber", e.target.value)}
            autoComplete="tel"
            placeholder="+1 555 000 0000"
          />

          <label htmlFor="address">
            Address <span style={{ fontWeight: 400, textTransform: "none" }}>(optional)</span>
          </label>
          <input
            id="address"
            value={form.address}
            onChange={(e) => update("address", e.target.value)}
            autoComplete="street-address"
            placeholder="123 Main St"
          />

          <button type="submit" className="btn-full" disabled={loading}>
            {loading ? (
              <>
                <span className="spinner" />
                Creating account…
              </>
            ) : (
              "Create account"
            )}
          </button>
        </form>

        <div className="auth-footer">
          Already have an account?{" "}
          <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
