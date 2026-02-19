export interface DashboardSummary {
  totalPayable: number;
  totalReceivable: number;
  totalRevenue: number;
  totalExpenses: number;
}

export interface CashFlowPoint {
  month: string;
  revenue: number;
  expense: number;
}

export interface RevenueExpenseItem {
  categoryName: string;
  groupName: string;
  total: number;
  type: 'REVENUE' | 'EXPENSE' | 'OTHER';
}

export interface MonthlyEvolutionPoint {
  month: string;
  revenue: number;
  expense: number;
  balance: number;
}

export interface DashboardPeriod {
  from: string; // yyyy-MM-dd
  to: string; // yyyy-MM-dd
}
