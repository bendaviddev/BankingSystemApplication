import { useState } from "react";
import { Link } from "react-router-dom";
import type { BankAccount } from "../types";
import {
  ACCOUNT_STATUS_LABELS,
  ACCOUNT_STATUS_TONE,
  ACCOUNT_TYPE_LABELS,
  formatDate,
  formatMoney,
  maskAccountNumber,
} from "../lib/format";
import { Badge } from "./ui/Badge";

interface AccountCardProps {
  account: BankAccount;
}

export function AccountCard({ account }: AccountCardProps) {
  const [revealed, setRevealed] = useState(false);
  const disabled = account.status !== "ACTIVE";

  return (
    <div className={`account-card ${disabled ? "account-card-disabled" : ""}`}>
      <div className="account-card-top">
        <span className={`type-badge type-${account.accountType}`}>
          {ACCOUNT_TYPE_LABELS[account.accountType]}
        </span>
        <Badge tone={ACCOUNT_STATUS_TONE[account.status]}>{ACCOUNT_STATUS_LABELS[account.status]}</Badge>
      </div>

      <div className="account-card-number">
        <span>{revealed ? account.accountNumber : maskAccountNumber(account.accountNumber)}</span>
        <button
          type="button"
          className="link-button"
          onClick={() => setRevealed((v) => !v)}
          aria-label={revealed ? "Hide account number" : "Reveal account number"}
        >
          {revealed ? "Hide" : "Reveal"}
        </button>
      </div>

      <div className="account-card-balance">{formatMoney(account.balance)}</div>
      <div className="account-card-meta">Opened {formatDate(account.createdAt)}</div>

      <Link
        to={`/app/accounts/${account.accountId}`}
        className="account-card-link"
        aria-disabled={disabled}
      >
        View details →
      </Link>
    </div>
  );
}
