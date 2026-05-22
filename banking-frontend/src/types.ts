export type UserRole = "USER" | "ADMIN";

export interface AuthSession {
  token: string;
  userId: number;
  username: string;
  role: UserRole;
}

export interface BankAccount {
  accountId: number;
  userId: number;
  accountNumber: string;
  accountType: "BASIC" | "SAVING";
  currency: string;
  balance: number;
  status: string;
}

export interface Transaction {
  transactionId: number;
  accountId: number;
  transactionType: "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";
  amount: number;
  description: string;
  createdAt: string;
}

export interface ApiMessage {
  message: string;
}
