import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TransactionTable } from "./TransactionTable";
import type { Transaction } from "../types";

const sample: Transaction = {
  transactionId: 1,
  reference: "TXN-abc123",
  accountId: 10,
  counterpartyAccountId: null,
  transactionType: "DEPOSIT",
  status: "COMPLETED",
  amount: 250.75,
  runningBalance: 1000,
  category: "INCOME",
  description: "Payroll deposit",
  memo: null,
  createdAt: "2026-06-01T12:00:00",
};

describe("TransactionTable", () => {
  it("shows a loading state", () => {
    render(<TransactionTable transactions={[]} loading />);
    expect(screen.getByText(/loading transactions/i)).toBeInTheDocument();
  });

  it("shows an error state with a retry action", async () => {
    const onRetry = vi.fn();
    render(<TransactionTable transactions={[]} error="Network error" onRetry={onRetry} />);
    expect(screen.getByText("Network error")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /try again/i }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("shows an empty state when there are no transactions", () => {
    render(<TransactionTable transactions={[]} emptyTitle="Nothing here" />);
    expect(screen.getByText("Nothing here")).toBeInTheDocument();
  });

  it("renders a row for each transaction with a signed, formatted amount", () => {
    render(<TransactionTable transactions={[sample]} />);
    expect(screen.getByText("+$250.75")).toBeInTheDocument();
    expect(screen.getByText("Payroll deposit")).toBeInTheDocument();
  });

  it("invokes onRowClick when a row is activated", async () => {
    const onRowClick = vi.fn();
    render(<TransactionTable transactions={[sample]} onRowClick={onRowClick} />);
    await userEvent.click(screen.getByText("Payroll deposit"));
    expect(onRowClick).toHaveBeenCalledWith(sample);
  });
});
