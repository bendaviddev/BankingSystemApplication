import { type FormEvent, useEffect, useState } from "react";
import { api } from "../api/client";
import type { BankAccountType } from "../types";
import { Button } from "./ui/Button";
import { Input } from "./ui/Input";
import { Modal } from "./ui/Modal";
import { Select } from "./ui/Select";

interface OpenAccountModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function OpenAccountModal({ open, onClose, onSuccess }: OpenAccountModalProps) {
  const [accountType, setAccountType] = useState<BankAccountType>("CHECKING");
  const [openingBalance, setOpeningBalance] = useState("0");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (open) {
      setAccountType("CHECKING");
      setOpeningBalance("0");
      setError("");
    }
  }, [open]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await api.openAccount({ accountType, openingBalance: parseFloat(openingBalance) || 0 });
      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not open account. You may have reached the 5-account limit.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Open a new account">
      <form onSubmit={handleSubmit} noValidate>
        {error && <div className="alert error">{error}</div>}
        <Select
          id="new-account-type"
          label="Account type"
          value={accountType}
          onChange={(e) => setAccountType(e.target.value as BankAccountType)}
        >
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
        </Select>
        <Input
          id="new-account-balance"
          label="Opening balance (USD)"
          type="number"
          min="0"
          step="0.01"
          value={openingBalance}
          onChange={(e) => setOpeningBalance(e.target.value)}
          placeholder="0.00"
        />
        <Button type="submit" fullWidth loading={submitting}>
          Open account
        </Button>
      </form>
    </Modal>
  );
}
