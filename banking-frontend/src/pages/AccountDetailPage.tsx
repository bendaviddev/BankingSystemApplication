import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/client";
import type { BankAccount, PagedResponse, Transaction } from "../types";
import {
  ACCOUNT_STATUS_LABELS,
  ACCOUNT_STATUS_TONE,
  ACCOUNT_TYPE_LABELS,
  formatDate,
  formatMoney,
} from "../lib/format";
import { Card } from "../components/ui/Card";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";
import { Pagination } from "../components/ui/Pagination";
import { TransactionTable } from "../components/TransactionTable";
import { useToast } from "../context/ToastContext";

export function AccountDetailPage() {
  const { id } = useParams<{ id: string }>();
  const accountId = Number(id);
  const navigate = useNavigate();
  const { toast } = useToast();

  const [account, setAccount] = useState<BankAccount | null>(null);
  const [txPage, setTxPage] = useState<PagedResponse<Transaction> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const load = useCallback(async () => {
    if (!accountId || Number.isNaN(accountId)) {
      setError("Invalid account.");
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [acct, tx] = await Promise.all([
        api.getAccount(accountId),
        api.getTransactions({ accountId, page, size: 10, sort: "date_desc" }),
      ]);
      setAccount(acct);
      setTxPage(tx);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load account.");
    } finally {
      setLoading(false);
    }
  }, [accountId, page]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCopy() {
    if (!account) return;
    try {
      await navigator.clipboard.writeText(account.accountNumber);
      setCopied(true);
      toast("Account number copied.", "success");
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast("Could not copy to clipboard.", "error");
    }
  }

  if (loading) return <Spinner label="Loading account…" />;
  if (error || !account) return <ErrorState message={error ?? "Account not found."} onRetry={load} />;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <Link to="/app/accounts" className="back-link">
            ← All accounts
          </Link>
          <h1>{ACCOUNT_TYPE_LABELS[account.accountType]} account</h1>
        </div>
        <Button variant="secondary" onClick={() => navigate("/app/transfer")}>
          Transfer
        </Button>
      </div>

      <Card>
        <div className="account-detail-top">
          <div>
            <div className="account-detail-number">
              {account.accountNumber}
              <button type="button" className="link-button" onClick={handleCopy}>
                {copied ? "Copied ✓" : "Copy"}
              </button>
            </div>
            <div className="account-detail-badges">
              <span className={`type-badge type-${account.accountType}`}>
                {ACCOUNT_TYPE_LABELS[account.accountType]}
              </span>
              <Badge tone={ACCOUNT_STATUS_TONE[account.status]}>
                {ACCOUNT_STATUS_LABELS[account.status]}
              </Badge>
            </div>
          </div>
          <div className="account-detail-balance">
            <div className="stat-label">Current balance</div>
            <div className="stat-value">{formatMoney(account.balance)}</div>
          </div>
        </div>
        <p className="muted">Opened {formatDate(account.createdAt)}</p>
      </Card>

      <Card>
        <h2>Transaction history</h2>
        <TransactionTable
          transactions={txPage?.items ?? []}
          emptyDescription="This account has no transactions yet."
        />
        {txPage && <Pagination page={page} totalPages={txPage.totalPages} onChange={setPage} />}
      </Card>
    </div>
  );
}
