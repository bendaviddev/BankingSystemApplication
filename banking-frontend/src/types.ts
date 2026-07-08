// Mirrors the v2 backend REST contract (see docs/ARCHITECTURE.md).

export type UserRole = "USER" | "ADMIN";

export interface AuthSession {
  token: string;
  userId: number;
  username: string;
  role: UserRole;
  firstName: string;
}

export interface MeProfile {
  userId: number;
  username: string;
  role: UserRole;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  address: string;
  createdAt: string;
}

export type BankAccountType = "CHECKING" | "SAVINGS";
export type BankAccountStatus = "ACTIVE" | "FROZEN" | "CLOSED" | "PENDING";
export type Currency = "DOLLAR" | "EURO" | "JAPANESE_YEN" | "GREAT_BRITISH_POUND";

export interface BankAccount {
  accountId: number;
  accountNumber: string;
  accountType: BankAccountType;
  currency: Currency;
  balance: number;
  status: BankAccountStatus;
  createdAt: string;
}

export interface AdminAccount extends BankAccount {
  userId: number;
  ownerUsername: string;
}

export type TransactionType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER_OUT" | "TRANSFER_IN";
export type TransactionStatus = "PENDING" | "COMPLETED" | "FAILED" | "REVERSED";
export type TransactionCategory =
  | "INCOME"
  | "TRANSFER"
  | "GROCERIES"
  | "DINING"
  | "RENT"
  | "UTILITIES"
  | "ENTERTAINMENT"
  | "SHOPPING"
  | "TRANSPORT"
  | "HEALTH"
  | "FEES"
  | "OTHER";

export interface Transaction {
  transactionId: number;
  reference: string;
  accountId: number;
  counterpartyAccountId: number | null;
  transactionType: TransactionType;
  status: TransactionStatus;
  amount: number;
  runningBalance: number | null;
  category: TransactionCategory;
  description: string | null;
  memo: string | null;
  createdAt: string;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface TransferResult {
  outLeg: Transaction;
  inLeg: Transaction;
}

export interface AccountLookupResult {
  maskedAccountNumber: string;
  ownerFirstName: string;
  ownerLastInitial: string;
}

export interface AnalyticsSummary {
  totalBalance: number;
  totalIn30d: number;
  totalOut30d: number;
  spendingByCategory: { category: TransactionCategory; total: number }[];
  monthlyFlow: { month: string; inflow: number; outflow: number }[];
  largestTransactions: Transaction[];
  balanceHistory: { date: string; balance: number }[];
}

export type AlertType =
  | "LOW_BALANCE"
  | "LARGE_TRANSACTION"
  | "TRANSFER_RECEIVED"
  | "TRANSACTION_FAILED"
  | "SECURITY";
export type AlertSeverity = "INFO" | "WARNING" | "CRITICAL";

export interface Alert {
  alertId: number;
  alertType: AlertType;
  severity: AlertSeverity;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface AlertsResponse {
  items: Alert[];
  unreadCount: number;
}

export interface AdminStats {
  totalUsers: number;
  totalAccounts: number;
  totalTransactions: number;
  totalVolume: number;
  failedTransactions24h: number;
}

export interface AdminUser {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  address: string;
  role: UserRole;
  createdAt: string;
}

export type ActivityType =
  | "LOGIN"
  | "LOGIN_FAILED"
  | "LOGOUT"
  | "VIEW_BANK_ACCOUNT"
  | "VIEW_TRANSACTION_HISTORY"
  | "CHANGE_PASSWORD"
  | "UPDATE_PERSONAL_INFORMATION"
  | "OPEN_NEW_BANK_ACCOUNT"
  | "CLOSE_BANK_ACCOUNT"
  | "DEPOSIT"
  | "WITHDRAWAL"
  | "TRANSFER"
  | "EXTERNAL_TRANSFER"
  | "ADMIN_ACCOUNT_STATUS_CHANGE";

export interface AuditLog {
  logId: number;
  userId: number;
  activityType: ActivityType;
  message: string;
  createdAt: string;
}

export interface ApiMessage {
  message: string;
}
