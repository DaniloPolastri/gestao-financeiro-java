import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { CashFlowPoint } from '../../models/dashboard.model';

@Component({
  selector: 'app-cash-flow-chart',
  templateUrl: './cash-flow-chart.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartModule],
})
export class CashFlowChartComponent {
  readonly data = input<CashFlowPoint[]>([]);
  readonly loading = input(false);

  protected readonly chartData = computed(() => {
    const points = this.data();
    return {
      labels: points.map((p) => p.month),
      datasets: [
        {
          label: 'Receitas',
          data: points.map((p) => p.revenue),
          borderColor: '#059669',
          backgroundColor: 'rgba(5,150,105,0.1)',
          fill: true,
          tension: 0.4,
        },
        {
          label: 'Despesas',
          data: points.map((p) => p.expense),
          borderColor: '#DC2626',
          backgroundColor: 'rgba(220,38,38,0.1)',
          fill: true,
          tension: 0.4,
        },
      ],
    };
  });

  protected readonly chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' as const } },
    scales: {
      y: {
        ticks: {
          callback: (v: number) => `R$ ${v.toLocaleString('pt-BR')}`,
        },
      },
    },
  };
}
