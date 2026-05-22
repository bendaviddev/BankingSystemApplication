import type { ApiMessage, AuthSession, BankAccount, Transaction } from "../types";

const API_BASE = import.meta.env.VITE_API_URL ?? "";

let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
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
    throw new Error(message);
  }

  return data;
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

  getAccounts() {
    return request<BankAccount[]>("/api/accounts");
  },

  openAccount(body: {
    accountType: "BASIC" | "SAVING";
    openingBalance: number;
  }) {
    return request<BankAccount>("/api/accounts", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  deposit(body: { accountId: number; amount: number }) {
    return request<ApiMessage>("/api/accounts/deposit", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  withdraw(body: { accountId: number; amount: number }) {
    return request<ApiMessage>("/api/accounts/withdraw", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  transfer(body: {
    fromAccountId: number;
    toAccountId: number;
    amount: number;
  }) {
    return request<ApiMessage>("/api/accounts/transfer", {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  getTransactions() {
    return request<Transaction[]>("/api/transactions");
  },
};
