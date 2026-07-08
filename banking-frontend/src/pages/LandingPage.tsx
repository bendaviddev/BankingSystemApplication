import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const FEATURES = [
  {
    icon: "⇄",
    title: "Instant transfers",
    body: "Move money between your own accounts or send to another user by account number, with a review step before anything is final.",
  },
  {
    icon: "◔",
    title: "Spending analytics",
    body: "Category breakdowns, monthly inflow vs. outflow, and balance trends — the kind of insight a real banking dashboard gives you.",
  },
  {
    icon: "◈",
    title: "Smart alerts",
    body: "Low-balance warnings, large-transaction notices, and security events land in an alert center so nothing surprises you.",
  },
  {
    icon: "🔒",
    title: "Security-minded",
    body: "Hashed sessions, bcrypt passwords, ownership checks on every request, and rate limiting — built like it matters, because good habits do.",
  },
];

const STEPS = [
  { title: "Create an account", body: "Register in seconds, or use the demo login below." },
  { title: "Open a bank account", body: "Spin up a checking or savings account with a starting balance." },
  { title: "Move money around", body: "Deposit, withdraw, and transfer — internally or to another demo user." },
  { title: "Explore the data", body: "Watch your dashboard, analytics, and alerts update in real time." },
];

export function LandingPage() {
  const { session } = useAuth();

  return (
    <div className="landing">
      <header className="landing-nav">
        <div className="brand">
          <div className="brand-icon">🏦</div>
          Ben Banking
        </div>
        <div className="landing-nav-actions">
          {session ? (
            <Link to="/app" className="btn btn-primary">
              Go to dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost">
                Sign in
              </Link>
              <Link to="/register" className="btn btn-primary">
                Get started
              </Link>
            </>
          )}
        </div>
      </header>

      <section className="landing-hero">
        <span className="demo-pill">Demo project — no real money</span>
        <h1 className="landing-title">Modern banking, demo edition.</h1>
        <p className="landing-subtitle">
          A full-stack banking app built to look and feel like the real thing: accounts, transfers, analytics,
          and alerts — powered by a Spring Boot API and a React dashboard.
        </p>
        <div className="landing-cta">
          <Link to="/register" className="btn btn-primary btn-lg">
            Create a free account
          </Link>
          <Link to="/login" className="btn btn-secondary btn-lg">
            Sign in
          </Link>
        </div>

        <div className="demo-callout">
          <div className="demo-callout-label">Try it instantly with the demo login</div>
          <div className="demo-callout-creds">
            <code>demo</code> / <code>Demo123!</code>
          </div>
          <p className="muted">Seeded with ~90 days of realistic transactions, multiple accounts, and alerts.</p>
        </div>
      </section>

      <section className="landing-section">
        <h2 className="landing-section-title">Everything a banking dashboard should have</h2>
        <div className="feature-grid">
          {FEATURES.map((f) => (
            <div key={f.title} className="feature-card">
              <div className="feature-icon" aria-hidden="true">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="landing-section">
        <h2 className="landing-section-title">How it works</h2>
        <div className="steps-grid">
          {STEPS.map((s, i) => (
            <div key={s.title} className="step-card">
              <div className="step-number">{i + 1}</div>
              <h3>{s.title}</h3>
              <p>{s.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="landing-section landing-final-cta">
        <h2 className="landing-section-title">Ready to take a look around?</h2>
        <p className="muted">No signup friction — jump straight in with the demo account, or register your own.</p>
        <div className="landing-cta">
          <Link to="/register" className="btn btn-primary btn-lg">
            Create a free account
          </Link>
          <Link to="/login" className="btn btn-secondary btn-lg">
            Sign in
          </Link>
        </div>
      </section>

      <footer className="landing-footer">
        <p>
          Ben Banking is a portfolio project. All balances, transfers, and users are simulated — no real money,
          no real payment rails.
        </p>
        <p className="muted">Built with Spring Boot, React, and TypeScript.</p>
      </footer>
    </div>
  );
}
