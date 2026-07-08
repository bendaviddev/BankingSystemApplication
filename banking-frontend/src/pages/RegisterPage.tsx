import { type FormEvent, useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { checkPasswordStrength } from "../lib/format";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";

export function RegisterPage() {
  const { session, login } = useAuth();
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

  if (session) {
    return <Navigate to="/app" replace />;
  }

  function update(field: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  const strength = checkPasswordStrength(form.password);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");

    if (form.password && !strength.valid) {
      setError("Password doesn't meet the strength requirements below.");
      return;
    }

    setLoading(true);
    try {
      const res = await api.register(form);
      login({
        token: res.token,
        userId: res.userId,
        username: res.username,
        role: res.role,
        firstName: res.firstName,
      });
      navigate("/app");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed. Please try again.");
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

        <h1 className="auth-heading">Create account</h1>
        <p className="auth-subtitle">Join Ben Banking in seconds — it's a demo, no real info required.</p>

        {error && <div className="alert error" role="alert">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="field-row">
            <Input
              id="firstName"
              label="First name"
              value={form.firstName}
              onChange={(e) => update("firstName", e.target.value)}
              required
              placeholder="Jane"
              autoFocus
            />
            <Input
              id="lastName"
              label="Last name"
              value={form.lastName}
              onChange={(e) => update("lastName", e.target.value)}
              required
              placeholder="Smith"
            />
          </div>

          <Input
            id="username"
            label="Username"
            value={form.username}
            onChange={(e) => update("username", e.target.value)}
            required
            autoComplete="username"
            placeholder="janesmith"
          />

          <Input
            id="password"
            label="Password"
            type="password"
            value={form.password}
            onChange={(e) => update("password", e.target.value)}
            required
            autoComplete="new-password"
            placeholder="••••••••"
          />
          <ul className="password-requirements">
            <li className={strength.minLength ? "met" : ""}>8+ characters</li>
            <li className={strength.hasUppercase ? "met" : ""}>Uppercase letter</li>
            <li className={strength.hasNumber ? "met" : ""}>Number</li>
            <li className={strength.hasSpecial ? "met" : ""}>Special character</li>
          </ul>

          <Input
            id="email"
            label="Email"
            type="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
            autoComplete="email"
            placeholder="jane@example.com"
          />

          <Input
            id="phone"
            label="Phone (optional)"
            type="tel"
            value={form.phoneNumber}
            onChange={(e) => update("phoneNumber", e.target.value)}
            autoComplete="tel"
            placeholder="+1 555 000 0000"
          />

          <Input
            id="address"
            label="Address (optional)"
            value={form.address}
            onChange={(e) => update("address", e.target.value)}
            autoComplete="street-address"
            placeholder="123 Main St"
          />

          <Button type="submit" fullWidth loading={loading}>
            Create account
          </Button>
        </form>

        <div className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
