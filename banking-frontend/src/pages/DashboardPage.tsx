import { type FormEvent, useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type { BankAccount, Transaction } from "../types";

type ActionTab = "deposit" | "withdraw" | "transfer" | "open";

const TAB_LABELS: Record<ActionTab, string> = {
  deposit: "Deposit",
  withdraw: "Withdraw",
  transfer: "Transfer",
  open: "Open Account",
};

const TX_LABELS: Record<string, string> = {
  DEPOSIT: "↑ Deposit",
  WITHDRAWAL: "↓ Withdraw",
  TRANSFER: "⇄ Transfer",
};

function formatMoney(n: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(n);
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function DashboardPage() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [tab, setTab] = useState<ActionTab>("deposit");
  const [amount, setAmount] = useState("");
  const [toAccountId, setToAccountId] = useState("");
  const [accountType, setAccountType] = useState<"BASIC" | "SAVING">("SAVING");
  const [openingBalance, setOpeningBalance] = useState("100");
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const refresh = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    setLoadError("");
    try {
      const [acct, tx] = await Promise.all([
        api.getAccounts(),
        api.getTransactions(),
      ]);
      setAccounts(acct);
      setTransactions(tx);
      if (acct.length > 0) {
        setSelectedId((prev) => prev ?? acct[0].accountId);
      }
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load data. Is the API running?");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    if (!session) {
      navigate("/login");
      return;
    }
    void refresh();
  }, [session, navigate, refresh]);

  async function handleAction(e: FormEvent) {
    e.preventDefault();
    setActionMessage("");
    setActionError("");
    setSubmitting(true);

    try {
      if (tab === "open") {
        await api.openAccount({
          accountType,
          openingBalance: parseFloat(openingBalance) || 0,
        });
        setActionMessage(`${accountType === "SAVING" ? "Savings" : "Basic"} account opened.`);
        setOpeningBalance("100");
      } else {
        if (!selectedId) throw new Error("Select an account first.");
        const value = parseFloat(amount);
        if (!value || value <= 0) throw new Error("Enter a valid amount greater than $0.");

        if (tab === "deposit") {
          await api.deposit({ accountId: selectedId, amount: value });
          setActionMessage(`Deposited ${formatMoney(value)} successfully.`);
        } else if (tab === "withdraw") {
          await api.withdraw({ accountId: selectedId, amount: value });
          setActionMessage(`Withdrew ${formatMoney(value)} successfully.`);
        } else {
          const toId = parseInt(toAccountId, 10);
          if (!toId) throw new Error("Enter a valid destination account ID.");
          await api.transfer({ fromAccountId: selectedId, toAccountId: toId, amount: value });
          setActionMessage(`Transferred ${formatMoney(value)} successfully.`);
        }
        setAmount("");
      }
      await refresh();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : "Action failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!session) return null;

  const totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0);
  const activeAccounts = accounts.filter((a) => a.status === "ACTIVE").length;

  return (
    <div className="app-shell">
      {/* ── Header ── */}
      <header className="topbar">
        <div className="brand">
          <div className="brand-icon">🏦</div>
          Ben Banking
        </div>
        <div className="topbar-right">
          <span className="user-chip">
            {session.username}
            {session.role === "ADMIN" && " · Admin"}
          </span>
          <button type="button" className="secondary" onClick={logout}>
            Sign out
          </button>
        </div>
      </header>

      {/* ── Load error ── */}
      {loadError && <div className="alert error" style={{ marginBottom: "1.25rem" }}>{loadError}</div>}

      {/* ── Summary bar ── */}
      {!loading && accounts.length > 0 && (
        <div className="summary-bar">
          <div className="summary-stat">
            <div className="stat-label">Total Balance</div>
            <div className="stat-value">{formatMoney(totalBalance)}</div>
            <div className="stat-sub">
              {activeAccounts} active account{activeAccounts !== 1 ? "s" : ""}
            </div>
          </div>
          <div className="summary-stat">
            <div className="stat-label">Transactions</div>
            <div className="stat-value">{transactions.length}</div>
            <div className="stat-sub">all time</div>
          </div>
        </div>
      )}

      {/* ── Accounts & Actions ── */}
      <div className="grid-2">
        {/* Accounts */}
        <section className="card">
          <h2>Your accounts</h2>
          {loading ? (
            <div className="loading-state">
              <span className="spinner" /> Loading accounts…
            </div>
          ) : accounts.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">🏦</div>
              <strong>No accounts yet</strong>
              <span>Open your first account using the panel on the right.</span>
            </div>
          ) : (
            <div className="accounts-list">
              {accounts.map((a) => (
                <button
                  key={a.accountId}
                  type="button"
                  className={`account-item ${selectedId === a.accountId ? "selected" : ""}`}
                  onClick={() => setSelectedId(a.accountId)}
                >
                  <div>
                    <div className="account-number">{a.accountNumber}</div>
                    <div className="account-meta">
                      <span className={`type-badge ${a.accountType}`}>
                        {a.accountType === "SAVING" ? "Savings" : "Basic"}
                      </span>
                      <span>{a.status}</span>
                    </div>
                  </div>
                  <div className="account-balance">
                    <div className="balance">{formatMoney(a.balance)}</div>
                    <div className="balance-label">USD</div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>

        {/* Actions */}
        <section className="card">
          <h2>Actions</h2>

          {actionMessage && <div className="alert success">{actionMessage}</div>}
          {actionError && <div className="alert error">{actionError}</div>}

          <div className="tabs">
            {(["deposit", "withdraw", "transfer", "open"] as ActionTab[]).map((t) => (
              <button
                key={t}
                type="button"
                className={tab === t ? "active" : ""}
                onClick={() => {
                  setTab(t);
                  setActionMessage("");
                  setActionError("");
                }}
              >
                {TAB_LABELS[t]}
              </button>
            ))}
          </div>

          <form onSubmit={handleAction}>
            {tab === "open" ? (
              <>
                <label htmlFor="accountType">Account type</label>
                <select
                  id="accountType"
                  value={accountType}
                  onChange={(e) => setAccountType(e.target.value as "BASIC" | "SAVING")}
                >
                  <option value="SAVING">Savings</option>
                  <option value="BASIC">Basic</option>
                </select>

                <label htmlFor="openingBalance">Opening deposit</label>
                <input
                  id="openingBalance"
                  type="number"
                  min="0"
                  step="0.01"
                  value={openingBalance}
                  onChange={(e) => setOpeningBalance(e.target.value)}
                  placeholder="0.00"
                />
              </>
            ) : (
              <>
                {accounts.length > 0 && (
                  <>
                    <label htmlFor="accountSelect">From account</label>
                    <select
                      id="accountSelect"
                      value={selectedId ?? ""}
                      onChange={(e) => setSelectedId(parseInt(e.target.value, 10))}
                    >
                      {accounts.map((a) => (
                        <option key={a.accountId} value={a.accountId}>
                          {a.accountNumber} — {formatMoney(a.balance)}
                        </option>
                      ))}
                    </select>
                  </>
                )}

                <label htmlFor="amount">Amount (USD)</label>
                <input
                  id="amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                  placeholder="0.00"
                />

                {tab === "transfer" && (
                  <>
                    <label htmlFor="toAccountId">To account ID</label>
                    <input
                      id="toAccountId"
                      type="number"
                      value={toAccountId}
                      onChange={(e) => setToAccountId(e.target.value)}
                      required
                      placeholder="Destination account ID"
                    />
                  </>
                )}
              </>
            )}

            <button type="submit" className="btn-full" disabled={submitting}>
              {submitting ? (
                <>
                  <span className="spinner" />
                  Processing…
                </>
              ) : (
                TAB_LABELS[tab]
              )}
            </button>
          </form>
        </section>
      </div>

      {/* ── Transactions ── */}
      <section className="card" style={{ marginTop: "1.25rem" }}>
        <h2>Recent transactions</h2>

        {loading ? (
          <div className="loading-state">
            <span className="spinner" /> Loading transactions…
          </div>
        ) : transactions.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📋</div>
            <strong>No transactions yet</strong>
            <span>Deposit or transfer funds to get started.</span>
          </div>
        ) : (
          <table className="transactions-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Account</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.transactionId}>
                  <td className="tx-date">{formatDate(t.createdAt)}</td>
                  <td>
                    <span className={`tx-type-badge ${t.transactionType}`}>
                      {TX_LABELS[t.transactionType] ?? t.transactionType}
                    </span>
                  </td>
                  <td className={`tx-amount ${t.transactionType}`}>
                    {t.transactionType === "WITHDRAWAL" ? "−" : "+"}
                    {formatMoney(t.amount)}
                  </td>
                  <td className="muted" style={{ fontFamily: "monospace", fontSize: "0.8rem" }}>
                    #{t.accountId}
                  </td>
                  <td className="tx-description">{t.description || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
