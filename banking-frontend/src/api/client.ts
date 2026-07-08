import type {
  AccountLookupResult,
  AdminAccount,
  AdminStats,
  AdminUser,
  Alert,
  AlertsResponse,
  AnalyticsSummary,
  ApiMessage,
  AuditLog,
  AuthSession,
  BankAccount,
  BankAccountStatus,
  BankAccountType,
  MeProfile,
  PagedResponse,
  Transaction,
  TransactionCategory,
  TransactionStatus,
  TransactionType,
  TransferResult,
} from "../types";

const API_BASE = import.meta.env.VITE_API_URL ?? "";

let authToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

/** Registered by AuthContext; invoked when an authenticated request comes back 401. */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options?.headers as Record<string, string> | undefined),
  };

  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    headers,
    ...options,
  });

  const text = await response.text();
  const data = text ? (JSON.parse(text) as T) : ({} as T);

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    if (typeof data === "object" && data !== null) {
      if ("message" in data) message = String((data as ApiMessage).message);
      else if ("error" in data) message = String((data as { error: string }).error);
    }

    if (response.status === 401 && authToken && onUnauthorized) {
      onUnauthorized();
    }

    throw new Error(message);
  }

  return data;
}

function qs(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  }
  const str = search.toString();
  return str ? `?${str}` : "";
}

export interface TransactionQuery {
  page?: number;
  size?: number;
  accountId?: number;
  type?: TransactionType;
  status?: TransactionStatus;
  category?: TransactionCategory;
  search?: string;
  from?: string;
  to?: string;
  sort?: "date_desc" | "date_asc" | "amount_desc" | "amount_asc";
}

export const api = {
  register(body: {
    username: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    email: string;
    address: string;
  }) {
    return request<AuthSession & ApiMessage>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  login(body: { username: string; password: string }) {
    return request<AuthSession & ApiMessage>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  logout() {
    return request<ApiMessage>("/api/auth/logout", { method: "POST" });
  },

  me() {
    return request<MeProfile>("/api/auth/me");
  },

  changePassword(body: { currentPassword: string; newPassword: string }) {
    return request<ApiMessage>("/api/auth/password", {
      method: "PUT",
      body: JSON.stringify(body),
    });
  },

  updateProfile(body: {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    address: string;
  }) {
    return request<MeProfile>("/api/profile", {
      method: "PUT",
      body: JSON.stringify(body),
    });
  },

  getAccounts() {
    return request<BankAccount[]>("/api/accounts");
  },

  getAccount(accountId: number) {
    return request<BankAccount>(`/api/accounts/${accountId}`);
  },

  openAccount(body: { accountType: BankAccountType; openingBalance: number }) {
    return request<BankAccount>("/api/accounts", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  lookupAccount(accountNumber: string) {
    return request<AccountLookupResult>(`/api/accounts/lookup${qs({ accountNumber })}`);
  },

  deposit(body: { accountId: number; amount: number; category?: TransactionCategory; memo?: string }) {
    return request<Transaction>("/api/accounts/deposit", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  withdraw(body: { accountId: number; amount: number; category?: TransactionCategory; memo?: string }) {
    return request<Transaction>("/api/accounts/withdraw", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  transfer(body: { fromAccountId: number; toAccountId: number; amount: number; memo?: string }) {
    return request<TransferResult>("/api/accounts/transfer", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  transferExternal(body: { fromAccountId: number; toAccountNumber: string; amount: number; memo?: string }) {
    return request<TransferResult>("/api/accounts/transfer/external", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  getTransactions(query: TransactionQuery = {}) {
    return request<PagedResponse<Transaction>>(`/api/transactions${qs(query as Record<string, string | number>)}`);
  },

  getTransaction(transactionId: number) {
    return request<Transaction>(`/api/transactions/${transactionId}`);
  },

  async exportTransactionsCsv(query: Omit<TransactionQuery, "page" | "size">): Promise<Blob> {
    const headers: Record<string, string> = {};
    if (authToken) headers.Authorization = `Bearer ${authToken}`;

    const response = await fetch(
      `${API_BASE}/api/transactions/export${qs(query as Record<string, string | number>)}`,
      { headers }
    );

    if (!response.ok) {
      throw new Error(`Export failed (${response.status})`);
    }

    return response.blob();
  },

  getAnalyticsSummary() {
    return request<AnalyticsSummary>("/api/analytics/summary");
  },

  getAlerts() {
    return request<AlertsResponse>("/api/alerts");
  },

  markAlertRead(alertId: number) {
    return request<ApiMessage>(`/api/alerts/${alertId}/read`, { method: "POST" });
  },

  markAllAlertsRead() {
    return request<ApiMessage>("/api/alerts/read-all", { method: "POST" });
  },

  // ── Admin ──
  getAdminStats() {
    return request<AdminStats>("/api/admin/stats");
  },

  getAdminUsers(search?: string) {
    return request<AdminUser[]>(`/api/admin/users${qs({ search })}`);
  },

  getAdminAccounts(search?: string) {
    return request<AdminAccount[]>(`/api/admin/accounts${qs({ search })}`);
  },

  getAdminTransactions(params: { status?: TransactionStatus; page?: number }) {
    return request<PagedResponse<Transaction>>(
      `/api/admin/transactions${qs(params as Record<string, string | number>)}`
    );
  },

  updateAccountStatus(accountId: number, status: BankAccountStatus) {
    return request<ApiMessage>(`/api/admin/accounts/${accountId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
  },

  getAuditLogs(page?: number) {
    return request<PagedResponse<AuditLog>>(`/api/admin/audit-logs${qs({ page })}`);
  },
};

export type { Alert };
