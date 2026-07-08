import type {
  AlertSeverity,
  AlertType,
  BankAccountStatus,
  BankAccountType,
  TransactionCategory,
  TransactionStatus,
  TransactionType,
} from "../types";

export function formatMoney(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(value);
}

export function formatSignedMoney(value: number, negative: boolean): string {
  const formatted = formatMoney(Math.abs(value));
  return negative ? `-${formatted}` : `+${formatted}`;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

export function formatShortDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });
}

/** Masks all but the last 4 characters of an account number, e.g. "•••• 4821". */
export function maskAccountNumber(accountNumber: string): string {
  if (!accountNumber || accountNumber.length <= 4) return accountNumber ?? "";
  const last4 = accountNumber.slice(-4);
  return `•••• ${last4}`;
}

export const ACCOUNT_TYPE_LABELS: Record<BankAccountType, string> = {
  CHECKING: "Checking",
  SAVINGS: "Savings",
};

export const ACCOUNT_STATUS_LABELS: Record<BankAccountStatus, string> = {
  ACTIVE: "Active",
  FROZEN: "Frozen",
  CLOSED: "Closed",
  PENDING: "Pending",
};

export const ACCOUNT_STATUS_TONE: Record<BankAccountStatus, "success" | "warning" | "danger" | "neutral"> = {
  ACTIVE: "success",
  FROZEN: "warning",
  CLOSED: "danger",
  PENDING: "neutral",
};

export const TRANSACTION_TYPE_LABELS: Record<TransactionType, string> = {
  DEPOSIT: "Deposit",
  WITHDRAWAL: "Withdrawal",
  TRANSFER_OUT: "Transfer out",
  TRANSFER_IN: "Transfer in",
};

export const TRANSACTION_TYPE_ICONS: Record<TransactionType, string> = {
  DEPOSIT: "↓",
  WITHDRAWAL: "↑",
  TRANSFER_OUT: "↗",
  TRANSFER_IN: "↙",
};

/** True when this transaction type reduces the account balance. */
export function isOutflow(type: TransactionType): boolean {
  return type === "WITHDRAWAL" || type === "TRANSFER_OUT";
}

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  PENDING: "Pending",
  COMPLETED: "Completed",
  FAILED: "Failed",
  REVERSED: "Reversed",
};

export const TRANSACTION_STATUS_TONE: Record<TransactionStatus, "success" | "warning" | "danger" | "neutral"> = {
  PENDING: "neutral",
  COMPLETED: "success",
  FAILED: "danger",
  REVERSED: "warning",
};

export const CATEGORY_LABELS: Record<TransactionCategory, string> = {
  INCOME: "Income",
  TRANSFER: "Transfer",
  GROCERIES: "Groceries",
  DINING: "Dining",
  RENT: "Rent",
  UTILITIES: "Utilities",
  ENTERTAINMENT: "Entertainment",
  SHOPPING: "Shopping",
  TRANSPORT: "Transport",
  HEALTH: "Health",
  FEES: "Fees",
  OTHER: "Other",
};

export const ALERT_TYPE_LABELS: Record<AlertType, string> = {
  LOW_BALANCE: "Low balance",
  LARGE_TRANSACTION: "Large transaction",
  TRANSFER_RECEIVED: "Transfer received",
  TRANSACTION_FAILED: "Transaction failed",
  SECURITY: "Security",
};

export const ALERT_SEVERITY_TONE: Record<AlertSeverity, "success" | "warning" | "danger" | "neutral"> = {
  INFO: "neutral",
  WARNING: "warning",
  CRITICAL: "danger",
};

export const ALERT_SEVERITY_ICON: Record<AlertSeverity, string> = {
  INFO: "ℹ",
  WARNING: "⚠",
  CRITICAL: "⛔",
};

/** Backend rule: >=8 chars, at least one uppercase letter, one digit, one non-alphanumeric char. */
export interface PasswordStrength {
  minLength: boolean;
  hasUppercase: boolean;
  hasNumber: boolean;
  hasSpecial: boolean;
  valid: boolean;
}

export function checkPasswordStrength(password: string): PasswordStrength {
  const minLength = password.length >= 8;
  const hasUppercase = /[A-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecial = /[^a-zA-Z0-9]/.test(password);
  return {
    minLength,
    hasUppercase,
    hasNumber,
    hasSpecial,
    valid: minLength && hasUppercase && hasNumber && hasSpecial,
  };
}
