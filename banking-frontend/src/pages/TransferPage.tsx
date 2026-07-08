import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useToast } from "../context/ToastContext";
import type { AccountLookupResult, BankAccount, TransferResult } from "../types";
import { formatMoney } from "../lib/format";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Select } from "../components/ui/Select";
import { Tabs } from "../components/ui/Tabs";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";

type Mode = "internal" | "external";
type Step = "form" | "review" | "result";

export function TransferPage() {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [mode, setMode] = useState<Mode>("internal");
  const [step, setStep] = useState<Step>("form");
  const [fromAccountId, setFromAccountId] = useState<number | "">("");
  const [toAccountId, setToAccountId] = useState<number | "">("");
  const [toAccountNumber, setToAccountNumber] = useState("");
  const [amount, setAmount] = useState("");
  const [memo, setMemo] = useState("");

  const [formError, setFormError] = useState("");
  const [lookupResult, setLookupResult] = useState<AccountLookupResult | null>(null);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<TransferResult | null>(null);
  const [resultError, setResultError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const acct = await api.getAccounts();
      const active = acct.filter((a) => a.status === "ACTIVE");
      setAccounts(active);
      setFromAccountId((prev) => prev || active[0]?.accountId || "");
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load accounts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const sourceAccount = accounts.find((a) => a.accountId === fromAccountId) ?? null;
  const destinationAccount =
    mode === "internal" ? accounts.find((a) => a.accountId === toAccountId) ?? null : null;

  function resetTransferState() {
    setAmount("");
    setMemo("");
    setToAccountId("");
    setToAccountNumber("");
    setLookupResult(null);
    setLookupError("");
    setFormError("");
  }

  function switchMode(next: string) {
    setMode(next as Mode);
    resetTransferState();
  }

  function validateForm(): string | null {
    if (!fromAccountId) return "Select a source account.";
    const value = parseFloat(amount);
    if (!value || value <= 0) return "Enter a valid amount greater than $0.";
    if (sourceAccount && value > sourceAccount.balance) {
      return `Amount exceeds available balance of ${formatMoney(sourceAccount.balance)}.`;
    }
    if (mode === "internal") {
      if (!toAccountId) return "Select a destination account.";
      if (toAccountId === fromAccountId) return "Choose a different destination account.";
    } else {
      if (!toAccountNumber.trim()) return "Enter the recipient's account number.";
    }
    if (memo.length > 140) return "Memo must be 140 characters or fewer.";
    return null;
  }

  async function handleContinue() {
    const validationError = validateForm();
    if (validationError) {
      setFormError(validationError);
      return;
    }
    setFormError("");

    if (mode === "external") {
      setLookupLoading(true);
      setLookupError("");
      try {
        const res = await api.lookupAccount(toAccountNumber.trim());
        setLookupResult(res);
        setStep("review");
      } catch (err) {
        setLookupError(
          err instanceof Error ? err.message : "Couldn't find an active account with that number."
        );
      } finally {
        setLookupLoading(false);
      }
    } else {
      setStep("review");
    }
  }

  async function handleConfirm() {
    setSubmitting(true);
    setResultError("");
    const value = parseFloat(amount);
    try {
      let res: TransferResult;
      if (mode === "internal") {
        res = await api.transfer({
          fromAccountId: Number(fromAccountId),
          toAccountId: Number(toAccountId),
          amount: value,
          memo: memo || undefined,
        });
      } else {
        res = await api.transferExternal({
          fromAccountId: Number(fromAccountId),
          toAccountNumber: toAccountNumber.trim(),
          amount: value,
          memo: memo || undefined,
        });
      }
      setResult(res);
      setStep("result");
      toast("Transfer completed.", "success");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Transfer failed.";
      setResultError(message);
      setStep("result");
      toast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  function handleNewTransfer() {
    resetTransferState();
    setResult(null);
    setResultError("");
    setStep("form");
    void load();
  }

  if (loading) return <Spinner label="Loading accounts…" />;
  if (loadError) return <ErrorState message={loadError} onRetry={load} />;

  if (accounts.length === 0) {
    return (
      <div className="page">
        <h1>Transfer</h1>
        <Card>
          <p>You need at least one active account to make a transfer.</p>
          <Button onClick={() => navigate("/app/accounts")}>Go to accounts</Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Transfer money</h1>

      <Card className="transfer-card">
        {step === "form" && (
          <>
            <Tabs
              tabs={[
                { key: "internal", label: "Between my accounts" },
                { key: "external", label: "To another user" },
              ]}
              active={mode}
              onChange={switchMode}
            />

            {formError && <div className="alert error">{formError}</div>}
            {lookupError && <div className="alert error">{lookupError}</div>}

            <Select
              id="from-account"
              label="From account"
              value={fromAccountId}
              onChange={(e) => setFromAccountId(Number(e.target.value))}
            >
              {accounts.map((a) => (
                <option key={a.accountId} value={a.accountId}>
                  {a.accountType} — {a.accountNumber} ({formatMoney(a.balance)})
                </option>
              ))}
            </Select>

            {mode === "internal" ? (
              <Select
                id="to-account"
                label="To account"
                value={toAccountId}
                onChange={(e) => setToAccountId(Number(e.target.value))}
              >
                <option value="">Select destination account…</option>
                {accounts
                  .filter((a) => a.accountId !== fromAccountId)
                  .map((a) => (
                    <option key={a.accountId} value={a.accountId}>
                      {a.accountType} — {a.accountNumber} ({formatMoney(a.balance)})
                    </option>
                  ))}
              </Select>
            ) : (
              <Input
                id="to-account-number"
                label="Recipient account number"
                value={toAccountNumber}
                onChange={(e) => setToAccountNumber(e.target.value)}
                placeholder="Enter the full account number"
                hint="Try transferring to another demo user's account."
              />
            )}

            <Input
              id="amount"
              label="Amount (USD)"
              type="number"
              min="0.01"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
            />

            <div className="field">
              <label htmlFor="memo">Memo (optional)</label>
              <input
                id="memo"
                value={memo}
                onChange={(e) => setMemo(e.target.value.slice(0, 140))}
                placeholder="What's this for?"
                maxLength={140}
              />
              <p className="field-hint">{memo.length}/140</p>
            </div>

            <Button fullWidth onClick={handleContinue} loading={lookupLoading}>
              Continue
            </Button>
          </>
        )}

        {step === "review" && (
          <div className="transfer-review">
            <h2>Review transfer</h2>
            <dl className="review-list">
              <div>
                <dt>From</dt>
                <dd>
                  {sourceAccount?.accountType} — {sourceAccount?.accountNumber}
                </dd>
              </div>
              <div>
                <dt>To</dt>
                <dd>
                  {mode === "internal"
                    ? `${destinationAccount?.accountType} — ${destinationAccount?.accountNumber}`
                    : `${lookupResult?.ownerFirstName} ${lookupResult?.ownerLastInitial}. — ${lookupResult?.maskedAccountNumber}`}
                </dd>
              </div>
              <div>
                <dt>Amount</dt>
                <dd className="review-amount">{formatMoney(parseFloat(amount) || 0)}</dd>
              </div>
              {memo && (
                <div>
                  <dt>Memo</dt>
                  <dd>{memo}</dd>
                </div>
              )}
            </dl>
            <div className="review-actions">
              <Button variant="secondary" onClick={() => setStep("form")} disabled={submitting}>
                Back
              </Button>
              <Button onClick={handleConfirm} loading={submitting}>
                Confirm transfer
              </Button>
            </div>
          </div>
        )}

        {step === "result" && (
          <div className="transfer-result">
            {result ? (
              <>
                <div className="result-icon success">✓</div>
                <h2>Transfer successful</h2>
                <p className="muted">Reference {result.outLeg.reference}</p>
                <dl className="review-list">
                  <div>
                    <dt>Amount sent</dt>
                    <dd>{formatMoney(result.outLeg.amount)}</dd>
                  </div>
                  <div>
                    <dt>New balance</dt>
                    <dd>{result.outLeg.runningBalance != null ? formatMoney(result.outLeg.runningBalance) : "—"}</dd>
                  </div>
                </dl>
              </>
            ) : (
              <>
                <div className="result-icon error">✕</div>
                <h2>Transfer failed</h2>
                <p className="muted">{resultError}</p>
              </>
            )}
            <div className="review-actions">
              <Button variant="secondary" onClick={() => navigate("/app/transactions")}>
                View transactions
              </Button>
              <Button onClick={handleNewTransfer}>Make another transfer</Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
