import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { formatMoney } from "../../lib/format";
import { EmptyState } from "../ui/EmptyState";

interface MonthlyFlowChartProps {
  data: { month: string; inflow: number; outflow: number }[];
}

export function MonthlyFlowChart({ data }: MonthlyFlowChartProps) {
  if (data.length === 0) {
    return <EmptyState icon="📊" title="No monthly data yet" description="Come back after a few months of activity." />;
  }

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
        <XAxis
          dataKey="month"
          tick={{ fill: "var(--muted)", fontSize: 12 }}
          axisLine={{ stroke: "var(--border)" }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: "var(--muted)", fontSize: 12 }}
          axisLine={false}
          tickLine={false}
          tickFormatter={(v: number) => formatMoney(v)}
          width={80}
        />
        <Tooltip
          contentStyle={{
            background: "var(--surface)",
            border: "1px solid var(--border)",
            borderRadius: 10,
            color: "var(--text)",
          }}
          formatter={(value: number, name: string) => [formatMoney(value), name === "inflow" ? "In" : "Out"]}
        />
        <Legend
          formatter={(value) => (value === "inflow" ? "Inflow" : "Outflow")}
          wrapperStyle={{ color: "var(--text-secondary)", fontSize: 12 }}
        />
        <Bar dataKey="inflow" fill="var(--success)" radius={[4, 4, 0, 0]} />
        <Bar dataKey="outflow" fill="var(--danger)" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
