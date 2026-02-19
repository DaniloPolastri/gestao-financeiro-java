import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { RevenueExpenseItem } from '../../models/dashboard.model';

@Component({
  selector: 'app-revenue-expense-chart',
  templateUrl: './revenue-expense-chart.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartModule],
})
export class RevenueExpenseChartComponent {
  readonly data = input<RevenueExpenseItem[]>([]);
  readonly loading = input(false);

  protected readonly chartData = computed(() => {
    const items = this.data();
    return {
      labels: items.map((i) => i.categoryName),
      datasets: [
        {
          label: 'Total',
          data: items.map((i) => i.total),
          backgroundColor: items.map((i) =>
            i.type === 'REVENUE'
              ? 'rgba(5,150,105,0.7)'
              : i.type === 'EXPENSE'
                ? 'rgba(220,38,38,0.7)'
                : 'rgba(107,114,128,0.7)',
          ),
        },
      ],
    };
  });

  protected readonly chartOptions = {
    indexAxis: 'y' as const,
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: {
        ticks: {
          callback: (v: number) => `R$ ${v.toLocaleString('pt-BR')}`,
        },
      },
    },
  };
}
