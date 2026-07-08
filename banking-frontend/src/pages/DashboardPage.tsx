import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import type { Alert, AnalyticsSummary, BankAccount, Transaction } from "../types";
import { ALERT_SEVERITY_ICON, ALERT_SEVERITY_TONE, ALERT_TYPE_LABELS, formatMoney } from "../lib/format";
import { StatCard } from "../components/ui/StatCard";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Modal } from "../components/ui/Modal";
import { Input } from "../components/ui/Input";
import { Select } from "../components/ui/Select";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";
import { EmptyState } from "../components/ui/EmptyState";
import { TransactionTable } from "../components/TransactionTable";
import { BalanceHistoryChart } from "../components/charts/BalanceHistoryChart";
import { OpenAccountModal } from "../components/OpenAccountModal";

export function DashboardPage() {
  const { profile } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [recentTx, setRecentTx] = useState<Transaction[]>([]);
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [depositOpen, setDepositOpen] = useState(false);
  const [openAccountOpen, setOpenAccountOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [acct, summaryRes, txRes, alertsRes] = await Promise.all([
        api.getAccounts(),
        api.getAnalyticsSummary(),
        api.getTransactions({ size: 5, sort: "date_desc" }),
        api.getAlerts(),
      ]);
      setAccounts(acct);
      setSummary(summaryRes);
      setRecentTx(txRes.items);
      setAlerts(alertsRes.items.slice(0, 3));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load dashboard data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return <Spinner label="Loading your dashboard…" />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={load} />;
  }

  const checkingTotal = accounts
    .filter((a) => a.accountType === "CHECKING")
    .reduce((sum, a) => sum + a.balance, 0);
  const savingsTotal = accounts
    .filter((a) => a.accountType === "SAVINGS")
    .reduce((sum, a) => sum + a.balance, 0);
  const totalBalance = summary?.totalBalance ?? accounts.reduce((sum, a) => sum + a.balance, 0);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Welcome back{profile?.firstName ? `, ${profile.firstName}` : ""}</h1>
          <p className="muted">Here's what's happening with your money.</p>
        </div>
      </div>

      <div className="stat-grid">
        <StatCard label="Total balance" value={formatMoney(totalBalance)} sub={`${accounts.length} account${accounts.length !== 1 ? "s" : ""}`} />
        <StatCard label="Checking" value={formatMoney(checkingTotal)} />
        <StatCard label="Savings" value={formatMoney(savingsTotal)} />
        <StatCard
          label="Last 30 days"
          value={`${formatMoney(summary?.totalIn30d ?? 0)} in`}
          sub={`${formatMoney(summary?.totalOut30d ?? 0)} out`}
          tone="success"
        />
      </div>

      <div className="quick-actions">
        <Button onClick={() => navigate("/app/transfer")}>Transfer</Button>
        <Button variant="secondary" onClick={() => setDepositOpen(true)} disabled={accounts.length === 0}>
          Deposit funds
        </Button>
        <Button variant="secondary" onClick={() => setOpenAccountOpen(true)}>
          Open account
        </Button>
        <Button variant="ghost" onClick={() => navigate("/app/analytics")}>
          View analytics
        </Button>
      </div>

      <div className="grid-2">
        <Card>
          <h2>Balance history</h2>
          <BalanceHistoryChart data={summary?.balanceHistory ?? []} />
        </Card>

        <Card>
          <h2>Alerts</h2>
          {alerts.length === 0 ? (
            <EmptyState icon="✅" title="You're all caught up" description="No unread alerts right now." />
          ) : (
            <ul className="alert-preview-list">
              {alerts.map((a) => (
                <li key={a.alertId} className={`alert-preview-item ${a.read ? "" : "unread"}`}>
                  <span className={`alert-icon tone-${ALERT_SEVERITY_TONE[a.severity]}`} aria-hidden="true">
                    {ALERT_SEVERITY_ICON[a.severity]}
                  </span>
                  <div>
                    <div className="alert-preview-title">{ALERT_TYPE_LABELS[a.alertType]}</div>
                    <div className="muted">{a.message}</div>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <Link to="/app/alerts" className="view-all-link">
            View all alerts →
          </Link>
        </Card>
      </div>

      <Card>
        <div className="card-header">
          <h2>Recent transactions</h2>
          <Link to="/app/transactions" className="view-all-link">
            View all →
          </Link>
        </div>
        <TransactionTable transactions={recentTx} showRunningBalance={false} />
      </Card>

      <DepositModal
        open={depositOpen}
        accounts={accounts}
        onClose={() => setDepositOpen(false)}
        onSuccess={() => {
          setDepositOpen(false);
          toast("Deposit successful.", "success");
          void load();
        }}
      />

      <OpenAccountModal
        open={openAccountOpen}
        onClose={() => setOpenAccountOpen(false)}
        onSuccess={() => {
          setOpenAccountOpen(false);
          toast("Account opened.", "success");
          void load();
        }}
      />
    </div>
  );
}

function DepositModal({
  open,
  accounts,
  onClose,
  onSuccess,
}: {
  open: boolean;
  accounts: BankAccount[];
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [accountId, setAccountId] = useState<number | "">("");
  const [amount, setAmount] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open) {
      setAccountId(accounts[0]?.accountId ?? "");
      setAmount("");
      setError("");
    }
  }, [open, accounts]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    const value = parseFloat(amount);
    if (!accountId) {
      setError("Select an account.");
      return;
    }
    if (!value || value <= 0) {
      setError("Enter a valid amount greater than $0.");
      return;
    }
    setSubmitting(true);
    try {
      await api.deposit({ accountId: Number(accountId), amount: value });
      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Deposit failed.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Deposit funds">
      <form onSubmit={handleSubmit} noValidate>
        {error && <div className="alert error">{error}</div>}
        <Select
          id="deposit-account"
          label="To account"
          value={accountId}
          onChange={(e) => setAccountId(Number(e.target.value))}
        >
          {accounts.map((a) => (
            <option key={a.accountId} value={a.accountId}>
              {a.accountType} — {a.accountNumber} ({formatMoney(a.balance)})
            </option>
          ))}
        </Select>
        <Input
          id="deposit-amount"
          label="Amount (USD)"
          type="number"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
          placeholder="0.00"
        />
        <Button type="submit" fullWidth loading={submitting}>
          Deposit
        </Button>
      </form>
    </Modal>
  );
}
