import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type {
  AdminAccount,
  AdminStats,
  AdminUser,
  AuditLog,
  BankAccountStatus,
  PagedResponse,
  Transaction,
  TransactionStatus,
} from "../types";
import {
  ACCOUNT_STATUS_LABELS,
  ACCOUNT_STATUS_TONE,
  formatDateTime,
  formatMoney,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_STATUS_TONE,
  TRANSACTION_TYPE_LABELS,
} from "../lib/format";
import { Card } from "../components/ui/Card";
import { StatCard } from "../components/ui/StatCard";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Select } from "../components/ui/Select";
import { Tabs } from "../components/ui/Tabs";
import { Badge } from "../components/ui/Badge";
import { Modal } from "../components/ui/Modal";
import { Pagination } from "../components/ui/Pagination";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";
import { EmptyState } from "../components/ui/EmptyState";
import { useToast } from "../context/ToastContext";

type AdminTab = "users" | "accounts" | "transactions" | "audit";

export function AdminPage() {
  const [tab, setTab] = useState<AdminTab>("users");
  const [stats, setStats] = useState<AdminStats | null>(null);

  useEffect(() => {
    void api.getAdminStats().then(setStats).catch(() => setStats(null));
  }, []);

  return (
    <div className="page">
      <h1>Admin</h1>

      {stats && (
        <div className="stat-grid">
          <StatCard label="Total users" value={String(stats.totalUsers)} />
          <StatCard label="Total accounts" value={String(stats.totalAccounts)} />
          <StatCard label="Total transactions" value={String(stats.totalTransactions)} />
          <StatCard label="Total volume" value={formatMoney(stats.totalVolume)} />
          <StatCard
            label="Failed (24h)"
            value={String(stats.failedTransactions24h)}
            tone={stats.failedTransactions24h > 0 ? "danger" : "default"}
          />
        </div>
      )}

      <Tabs
        tabs={[
          { key: "users", label: "Users" },
          { key: "accounts", label: "Accounts" },
          { key: "transactions", label: "Transactions" },
          { key: "audit", label: "Audit log" },
        ]}
        active={tab}
        onChange={(k) => setTab(k as AdminTab)}
      />

      {tab === "users" && <UsersTab />}
      {tab === "accounts" && <AccountsTab />}
      {tab === "transactions" && <TransactionsTab />}
      {tab === "audit" && <AuditLogTab />}
    </div>
  );
}

function UsersTab() {
  const [search, setSearch] = useState("");
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (q: string) => {
    setLoading(true);
    setError(null);
    try {
      setUsers(await api.getAdminUsers(q || undefined));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const handle = setTimeout(() => void load(search), 300);
    return () => clearTimeout(handle);
  }, [search, load]);

  return (
    <Card>
      <Input id="user-search" label="Search users" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Username, name, or email" />
      {loading ? (
        <Spinner label="Loading users…" />
      ) : error ? (
        <ErrorState message={error} onRetry={() => load(search)} />
      ) : users.length === 0 ? (
        <EmptyState title="No users found" />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Joined</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}</td>
                  <td>{u.firstName} {u.lastName}</td>
                  <td className="muted">{u.email || "—"}</td>
                  <td><Badge tone={u.role === "ADMIN" ? "accent" : "neutral"}>{u.role}</Badge></td>
                  <td className="muted">{formatDateTime(u.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function AccountsTab() {
  const { toast } = useToast();
  const [search, setSearch] = useState("");
  const [accounts, setAccounts] = useState<AdminAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState<{ account: AdminAccount; status: BankAccountStatus } | null>(null);
  const [confirming, setConfirming] = useState(false);

  const load = useCallback(async (q: string) => {
    setLoading(true);
    setError(null);
    try {
      setAccounts(await api.getAdminAccounts(q || undefined));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load accounts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const handle = setTimeout(() => void load(search), 300);
    return () => clearTimeout(handle);
  }, [search, load]);

  async function confirmStatusChange() {
    if (!pending) return;
    setConfirming(true);
    try {
      await api.updateAccountStatus(pending.account.accountId, pending.status);
      toast(`Account ${pending.account.accountNumber} updated to ${pending.status}.`, "success");
      setPending(null);
      void load(search);
    } catch (err) {
      toast(err instanceof Error ? err.message : "Failed to update account.", "error");
    } finally {
      setConfirming(false);
    }
  }

  return (
    <Card>
      <Input id="account-search" label="Search accounts" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Account number or owner username" />
      {loading ? (
        <Spinner label="Loading accounts…" />
      ) : error ? (
        <ErrorState message={error} onRetry={() => load(search)} />
      ) : accounts.length === 0 ? (
        <EmptyState title="No accounts found" />
      ) : (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Account</th>
                <th>Owner</th>
                <th>Type</th>
                <th>Balance</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((a) => (
                <tr key={a.accountId}>
                  <td className="mono">{a.accountNumber}</td>
                  <td>{a.ownerUsername}</td>
                  <td>{a.accountType}</td>
                  <td>{formatMoney(a.balance)}</td>
                  <td><Badge tone={ACCOUNT_STATUS_TONE[a.status]}>{ACCOUNT_STATUS_LABELS[a.status]}</Badge></td>
                  <td className="admin-actions">
                    {a.status !== "FROZEN" && a.status !== "CLOSED" && (
                      <Button size="sm" variant="secondary" onClick={() => setPending({ account: a, status: "FROZEN" })}>
                        Freeze
                      </Button>
                    )}
                    {a.status === "FROZEN" && (
                      <Button size="sm" variant="secondary" onClick={() => setPending({ account: a, status: "ACTIVE" })}>
                        Unfreeze
                      </Button>
                    )}
                    {a.status !== "CLOSED" && (
                      <Button size="sm" variant="danger" onClick={() => setPending({ account: a, status: "CLOSED" })}>
                        Close
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={pending !== null}
        onClose={() => setPending(null)}
        title="Confirm status change"
        footer={
          <>
            <Button variant="secondary" onClick={() => setPending(null)} disabled={confirming}>
              Cancel
            </Button>
            <Button variant={pending?.status === "CLOSED" ? "danger" : "primary"} onClick={confirmStatusChange} loading={confirming}>
              Confirm
            </Button>
          </>
        }
      >
        {pending && (
          <p>
            Set account <strong>{pending.account.accountNumber}</strong> (owner {pending.account.ownerUsername}) to{" "}
            <strong>{ACCOUNT_STATUS_LABELS[pending.status]}</strong>?
          </p>
        )}
      </Modal>
    </Card>
  );
}

function TransactionsTab() {
  const [status, setStatus] = useState<TransactionStatus | "">("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PagedResponse<Transaction> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await api.getAdminTransactions({ status: status || undefined, page }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load transactions.");
    } finally {
      setLoading(false);
    }
  }, [status, page]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    setPage(0);
  }, [status]);

  return (
    <Card>
      <Select id="admin-tx-status" label="Status" value={status} onChange={(e) => setStatus(e.target.value as TransactionStatus | "")}>
        <option value="">All statuses</option>
        {Object.entries(TRANSACTION_STATUS_LABELS).map(([value, label]) => (
          <option key={value} value={value}>{label}</option>
        ))}
      </Select>

      {loading ? (
        <Spinner label="Loading transactions…" />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState title="No transactions found" />
      ) : (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Account</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Amount</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((t) => (
                  <tr key={t.transactionId}>
                    <td className="mono">{t.reference}</td>
                    <td>#{t.accountId}</td>
                    <td>{TRANSACTION_TYPE_LABELS[t.transactionType]}</td>
                    <td><Badge tone={TRANSACTION_STATUS_TONE[t.status]}>{TRANSACTION_STATUS_LABELS[t.status]}</Badge></td>
                    <td>{formatMoney(t.amount)}</td>
                    <td className="muted">{formatDateTime(t.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </Card>
  );
}

function AuditLogTab() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PagedResponse<AuditLog> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await api.getAuditLogs(page));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load audit log.");
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <Card>
      {loading ? (
        <Spinner label="Loading audit log…" />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : !data || data.items.length === 0 ? (
        <EmptyState title="No audit entries" />
      ) : (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Activity</th>
                  <th>Message</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((log) => (
                  <tr key={log.logId}>
                    <td>#{log.userId}</td>
                    <td><Badge>{log.activityType}</Badge></td>
                    <td className="muted">{log.message}</td>
                    <td className="muted">{formatDateTime(log.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </Card>
  );
}
