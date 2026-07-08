import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { BankAccount } from "../types";
import { AccountCard } from "../components/AccountCard";
import { OpenAccountModal } from "../components/OpenAccountModal";
import { Button } from "../components/ui/Button";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";
import { EmptyState } from "../components/ui/EmptyState";
import { useToast } from "../context/ToastContext";

export function AccountsPage() {
  const { toast } = useToast();
  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const acct = await api.getAccounts();
      setAccounts(acct);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load accounts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Accounts</h1>
          <p className="muted">Manage your checking and savings accounts.</p>
        </div>
        <Button onClick={() => setModalOpen(true)} disabled={accounts.length >= 5}>
          Open account
        </Button>
      </div>

      {loading ? (
        <Spinner label="Loading accounts…" />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : accounts.length === 0 ? (
        <EmptyState
          icon="🏦"
          title="No accounts yet"
          description="Open your first account to start banking."
          action={
            <Button onClick={() => setModalOpen(true)} size="sm">
              Open account
            </Button>
          }
        />
      ) : (
        <div className="account-grid">
          {accounts.map((a) => (
            <AccountCard key={a.accountId} account={a} />
          ))}
        </div>
      )}

      {accounts.length >= 5 && (
        <p className="muted account-limit-note">You've reached the 5-account limit.</p>
      )}

      <OpenAccountModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={() => {
          setModalOpen(false);
          toast("Account opened.", "success");
          void load();
        }}
      />
    </div>
  );
}
