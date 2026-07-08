import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { TransactionCategory } from "../../types";
import { CATEGORY_LABELS, formatMoney } from "../../lib/format";
import { EmptyState } from "../ui/EmptyState";

interface CategoryDonutChartProps {
  data: { category: TransactionCategory; total: number }[];
}

const PALETTE = [
  "var(--chart-1)",
  "var(--chart-2)",
  "var(--chart-3)",
  "var(--chart-4)",
  "var(--chart-5)",
  "var(--chart-6)",
  "var(--chart-7)",
  "var(--chart-8)",
];

export function CategoryDonutChart({ data }: CategoryDonutChartProps) {
  const spendData = data.filter((d) => d.category !== "INCOME" && d.total > 0);

  if (spendData.length === 0) {
    return <EmptyState icon="🍩" title="No spending yet" description="Categorized spending will show up here." />;
  }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie
          data={spendData}
          dataKey="total"
          nameKey="category"
          innerRadius="55%"
          outerRadius="85%"
          paddingAngle={2}
        >
          {spendData.map((entry, index) => (
            <Cell key={entry.category} fill={PALETTE[index % PALETTE.length]} />
          ))}
        </Pie>
        <Tooltip
          contentStyle={{
            background: "var(--surface)",
            border: "1px solid var(--border)",
            borderRadius: 10,
            color: "var(--text)",
          }}
          formatter={(value: number, _name: string, item) => [
            formatMoney(value),
            CATEGORY_LABELS[item.payload.category as TransactionCategory],
          ]}
        />
        <Legend
          formatter={(_value, entry) => {
            const category = (entry.payload as unknown as { category: TransactionCategory })?.category;
            return category ? CATEGORY_LABELS[category] : "";
          }}
          wrapperStyle={{ color: "var(--text-secondary)", fontSize: 12 }}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}
