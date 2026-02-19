import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';
import {
  CashFlowPoint,
  DashboardPeriod,
  DashboardSummary,
  MonthlyEvolutionPoint,
  RevenueExpenseItem,
} from '../models/dashboard.model';

export interface DashboardData {
  summary: DashboardSummary;
  cashFlow: CashFlowPoint[];
  revenueExpense: RevenueExpenseItem[];
  monthlyEvolution: MonthlyEvolutionPoint[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/dashboard';

  loadAll(period: DashboardPeriod): Observable<DashboardData> {
    const params = new HttpParams()
      .set('from', period.from)
      .set('to', period.to);

    return forkJoin({
      summary: this.http.get<DashboardSummary>(`${this.base}/summary`, { params }),
      cashFlow: this.http.get<CashFlowPoint[]>(`${this.base}/cash-flow`, { params }),
      revenueExpense: this.http.get<RevenueExpenseItem[]>(`${this.base}/revenue-expense`, { params }),
      monthlyEvolution: this.http.get<MonthlyEvolutionPoint[]>(`${this.base}/monthly-evolution`, { params }),
    });
  }
}
