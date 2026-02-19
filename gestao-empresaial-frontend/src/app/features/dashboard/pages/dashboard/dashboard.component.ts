import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  DashboardService,
  DashboardData,
} from '../../services/dashboard.service';
import { DashboardPeriod } from '../../models/dashboard.model';
import { DashboardFilterComponent } from '../../components/dashboard-filter/dashboard-filter.component';
import { SummaryCardsComponent } from '../../components/summary-cards/summary-cards.component';
import { CashFlowChartComponent } from '../../components/cash-flow-chart/cash-flow-chart.component';
import { RevenueExpenseChartComponent } from '../../components/revenue-expense-chart/revenue-expense-chart.component';
import { MonthlyEvolutionChartComponent } from '../../components/monthly-evolution-chart/monthly-evolution-chart.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DashboardFilterComponent,
    SummaryCardsComponent,
    CashFlowChartComponent,
    RevenueExpenseChartComponent,
    MonthlyEvolutionChartComponent,
  ],
})
export class DashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private currentRequest?: Subscription;

  protected readonly loading = signal(true);
  protected readonly dashboardData = signal<DashboardData | null>(null);
  protected readonly period = signal<DashboardPeriod>(
    this.currentMonthPeriod(),
  );

  constructor() {
    effect(() => {
      const p = this.period();
      untracked(() => this.loadData(p));
    });
  }

  protected onPeriodChange(period: DashboardPeriod): void {
    this.period.set(period);
  }

  private loadData(period: DashboardPeriod): void {
    this.currentRequest?.unsubscribe();
    this.loading.set(true);

    this.currentRequest = this.dashboardService
      .loadAll(period)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.dashboardData.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Dashboard load failed:', err);
          this.loading.set(false);
        },
      });
  }

  private currentMonthPeriod(): DashboardPeriod {
    const today = new Date();
    const from = new Date(today.getFullYear(), today.getMonth(), 1);
    const fmt = (d: Date) => d.toISOString().split('T')[0];
    return { from: fmt(from), to: fmt(today) };
  }
}
