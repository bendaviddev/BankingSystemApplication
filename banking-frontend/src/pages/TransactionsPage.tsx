import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api, type TransactionQuery } from "../api/client";
import type { Transaction } from "../types";
import {
  CATEGORY_LABELS,
  formatDateTime,
  formatMoney,
  isOutflow,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_TYPE_LABELS,
} from "../lib/format";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";
import { Select } from "../components/ui/Select";
import { Modal } from "../components/ui/Modal";
import { Pagination } from "../components/ui/Pagination";
import { TransactionTable } from "../components/TransactionTable";
import { useToast } from "../context/ToastContext";

const SORT_OPTIONS: { value: NonNullable<TransactionQuery["sort"]>; label: string }[] = [
  { value: "date_desc", label: "Newest first" },
  { value: "date_asc", label: "Oldest first" },
  { value: "amount_desc", label: "Amount: high to low" },
  { value: "amount_asc", label: "Amount: low to high" },
];

export function TransactionsPage() {
  const { toast } = useToast();
  const [searchParams] = useSearchParams();

  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");
  const [category, setCategory] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [sort, setSort] = useState<NonNullable<TransactionQuery["sort"]>>("date_desc");
  const [page, setPage] = useState(0);

  const [items, setItems] = useState<Transaction[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<Transaction | null>(null);
  const [exporting, setExporting] = useState(false);

  // Preset account filter from ?accountId= (used by Account detail "view all" links)
  const presetAccountId = searchParams.get("accountId");

  useEffect(() => {
    const handle = setTimeout(() => setDebouncedSearch(search), 350);
    return () => clearTimeout(handle);
  }, [search]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, type, status, category, from, to, sort]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const query: TransactionQuery = {
        page,
        size: 20,
        search: debouncedSearch || undefined,
        type: (type || undefined) as TransactionQuery["type"],
        status: (status || undefined) as TransactionQuery["status"],
        category: (category || undefined) as TransactionQuery["category"],
        from: from || undefined,
        to: to || undefined,
        sort,
        accountId: presetAccountId ? Number(presetAccountId) : undefined,
      };
      const res = await api.getTransactions(query);
      setItems(res.items);
      setTotalPages(res.totalPages);
      setTotalItems(res.totalItems);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load transactions.");
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch, type, status, category, from, to, sort, presetAccountId]);

  useEffect(() => {
    void load();
  }, [load]);

  const hasActiveFilters = Boolean(
    debouncedSearch || type || status || category || from || to || presetAccountId
  );

  function clearFilters() {
    setSearch("");
    setType("");
    setStatus("");
    setCategory("");
    setFrom("");
    setTo("");
    setSort("date_desc");
  }

  async function handleExport() {
    setExporting(true);
    try {
      const blob = await api.exportTransactionsCsv({
        search: debouncedSearch || undefined,
        type: (type || undefined) as TransactionQuery["type"],
        status: (status || undefined) as TransactionQuery["status"],
        category: (category || undefined) as TransactionQuery["category"],
        from: from || undefined,
        to: to || undefined,
        sort,
        accountId: presetAccountId ? Number(presetAccountId) : undefined,
      });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "transactions.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      toast("Export downloaded.", "success");
    } catch (err) {
      toast(err instanceof Error ? err.message : "Export failed.", "error");
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Transactions</h1>
          <p className="muted">{totalItems} total</p>
        </div>
        <Button variant="secondary" onClick={handleExport} loading={exporting}>
          Export CSV
        </Button>
      </div>

      <Card className="filter-bar">
        <Input
          id="tx-search"
          label="Search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Description, memo, or reference"
        />
        <Select id="tx-type" label="Type" value={type} onChange={(e) => setType(e.target.value)}>
          <option value="">All types</option>
          {Object.entries(TRANSACTION_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        <Select id="tx-status" label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All statuses</option>
          {Object.entries(TRANSACTION_STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        <Select id="tx-category" label="Category" value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
        <Input id="tx-from" label="From" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        <Input id="tx-to" label="To" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        <Select
          id="tx-sort"
          label="Sort by"
          value={sort}
          onChange={(e) => setSort(e.target.value as NonNullable<TransactionQuery["sort"]>)}
        >
          {SORT_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </Select>
        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters} className="clear-filters-btn">
            Clear filters
          </Button>
        )}
      </Card>

      <Card>
        <TransactionTable
          transactions={items}
          loading={loading}
          error={error}
          onRetry={load}
          onRowClick={setSelected}
          emptyTitle={hasActiveFilters ? "No matches" : "No transactions yet"}
          emptyDescription={
            hasActiveFilters
              ? "Try adjusting or clearing your filters."
              : "Deposit, withdraw, or transfer funds to see activity here."
          }
        />
        {!loading && !error && <Pagination page={page} totalPages={totalPages} onChange={setPage} />}
      </Card>

      <Modal open={selected !== null} onClose={() => setSelected(null)} title="Transaction detail">
        {selected && (
          <dl className="review-list">
            <div>
              <dt>Reference</dt>
              <dd>{selected.reference}</dd>
            </div>
            <div>
              <dt>Type</dt>
              <dd>{TRANSACTION_TYPE_LABELS[selected.transactionType]}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{TRANSACTION_STATUS_LABELS[selected.status]}</dd>
            </div>
            <div>
              <dt>Amount</dt>
              <dd className={isOutflow(selected.transactionType) ? "tx-amount-out" : "tx-amount-in"}>
                {isOutflow(selected.transactionType) ? "−" : "+"}
                {formatMoney(selected.amount)}
              </dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{CATEGORY_LABELS[selected.category]}</dd>
            </div>
            {selected.runningBalance != null && (
              <div>
                <dt>Balance after</dt>
                <dd>{formatMoney(selected.runningBalance)}</dd>
              </div>
            )}
            {selected.counterpartyAccountId != null && (
              <div>
                <dt>Counterparty account</dt>
                <dd>#{selected.counterpartyAccountId}</dd>
              </div>
            )}
            {selected.memo && (
              <div>
                <dt>Memo</dt>
                <dd>{selected.memo}</dd>
              </div>
            )}
            {selected.description && (
              <div>
                <dt>Description</dt>
                <dd>{selected.description}</dd>
              </div>
            )}
            <div>
              <dt>Date</dt>
              <dd>{formatDateTime(selected.createdAt)}</dd>
            </div>
          </dl>
        )}
      </Modal>
    </div>
  );
}
