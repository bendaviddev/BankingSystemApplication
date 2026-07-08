import type { Transaction } from "../types";
import {
  CATEGORY_LABELS,
  formatDate,
  formatMoney,
  isOutflow,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_STATUS_TONE,
  TRANSACTION_TYPE_ICONS,
  TRANSACTION_TYPE_LABELS,
} from "../lib/format";
import { Badge } from "./ui/Badge";
import { EmptyState } from "./ui/EmptyState";
import { ErrorState } from "./ui/ErrorState";
import { Spinner } from "./ui/Spinner";

interface TransactionTableProps {
  transactions: Transaction[];
  loading?: boolean;
  error?: string | null;
  onRetry?: () => void;
  onRowClick?: (transaction: Transaction) => void;
  emptyTitle?: string;
  emptyDescription?: string;
  showRunningBalance?: boolean;
}

export function TransactionTable({
  transactions,
  loading = false,
  error = null,
  onRetry,
  onRowClick,
  emptyTitle = "No transactions yet",
  emptyDescription = "Deposit, withdraw, or transfer funds to see activity here.",
  showRunningBalance = true,
}: TransactionTableProps) {
  if (loading) {
    return <Spinner label="Loading transactions…" />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={onRetry} />;
  }

  if (transactions.length === 0) {
    return <EmptyState icon="📋" title={emptyTitle} description={emptyDescription} />;
  }

  return (
    <div className="table-scroll">
      <table className="transactions-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Category</th>
            <th>Status</th>
            <th>Amount</th>
            {showRunningBalance && <th>Balance</th>}
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((t) => {
            const negative = isOutflow(t.transactionType);
            return (
              <tr
                key={t.transactionId}
                className={onRowClick ? "clickable-row" : ""}
                onClick={() => onRowClick?.(t)}
                tabIndex={onRowClick ? 0 : undefined}
                role={onRowClick ? "button" : undefined}
                onKeyDown={(e) => {
                  if (onRowClick && (e.key === "Enter" || e.key === " ")) {
                    e.preventDefault();
                    onRowClick(t);
                  }
                }}
              >
                <td className="tx-date">{formatDate(t.createdAt)}</td>
                <td>
                  <span className={`tx-type-badge tx-type-${t.transactionType}`}>
                    <span aria-hidden="true">{TRANSACTION_TYPE_ICONS[t.transactionType]}</span>
                    {TRANSACTION_TYPE_LABELS[t.transactionType]}
                  </span>
                </td>
                <td>
                  <Badge tone="neutral">{CATEGORY_LABELS[t.category]}</Badge>
                </td>
                <td>
                  <Badge tone={TRANSACTION_STATUS_TONE[t.status]}>
                    {TRANSACTION_STATUS_LABELS[t.status]}
                  </Badge>
                </td>
                <td className={`tx-amount ${negative ? "tx-amount-out" : "tx-amount-in"}`}>
                  {negative ? "−" : "+"}
                  {formatMoney(t.amount)}
                </td>
                {showRunningBalance && (
                  <td className="muted tx-balance">
                    {t.runningBalance != null ? formatMoney(t.runningBalance) : "—"}
                  </td>
                )}
                <td className="tx-description">{t.memo || t.description || "—"}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
