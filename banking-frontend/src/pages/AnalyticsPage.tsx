import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { AnalyticsSummary } from "../types";
import { formatMoney } from "../lib/format";
import { Card } from "../components/ui/Card";
import { StatCard } from "../components/ui/StatCard";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";
import { TransactionTable } from "../components/TransactionTable";
import { BalanceHistoryChart } from "../components/charts/BalanceHistoryChart";
import { MonthlyFlowChart } from "../components/charts/MonthlyFlowChart";
import { CategoryDonutChart } from "../components/charts/CategoryDonutChart";

export function AnalyticsPage() {
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.getAnalyticsSummary();
      setSummary(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load analytics.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) return <Spinner label="Loading analytics…" />;
  if (error || !summary) return <ErrorState message={error ?? "No data."} onRetry={load} />;

  const net = summary.totalIn30d - summary.totalOut30d;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Analytics</h1>
          <p className="muted">A look at your money over the last 30 days.</p>
        </div>
      </div>

      <div className="stat-grid">
        <StatCard label="30-day inflow" value={formatMoney(summary.totalIn30d)} tone="success" />
        <StatCard label="30-day outflow" value={formatMoney(summary.totalOut30d)} tone="danger" />
        <StatCard label="Net" value={`${net >= 0 ? "+" : ""}${formatMoney(net)}`} tone={net >= 0 ? "success" : "danger"} />
        <StatCard label="Total balance" value={formatMoney(summary.totalBalance)} />
      </div>

      <div className="grid-2">
        <Card>
          <h2>Spending by category</h2>
          <CategoryDonutChart data={summary.spendingByCategory} />
        </Card>
        <Card>
          <h2>Monthly inflow vs. outflow</h2>
          <MonthlyFlowChart data={summary.monthlyFlow} />
        </Card>
      </div>

      <Card>
        <h2>Balance history (30 days)</h2>
        <BalanceHistoryChart data={summary.balanceHistory} />
      </Card>

      <Card>
        <h2>Largest transactions</h2>
        <TransactionTable transactions={summary.largestTransactions} showRunningBalance={false} />
      </Card>
    </div>
  );
}
