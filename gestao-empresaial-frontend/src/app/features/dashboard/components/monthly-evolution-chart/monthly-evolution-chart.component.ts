import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { MonthlyEvolutionPoint } from '../../models/dashboard.model';

@Component({
  selector: 'app-monthly-evolution-chart',
  templateUrl: './monthly-evolution-chart.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartModule],
})
export class MonthlyEvolutionChartComponent {
  readonly data = input<MonthlyEvolutionPoint[]>([]);
  readonly loading = input(false);

  protected readonly chartData = computed(() => {
    const points = this.data();
    return {
      labels: points.map((p) => p.month),
      datasets: [
        {
          type: 'bar' as const,
          label: 'Receitas',
          data: points.map((p) => p.revenue),
          backgroundColor: 'rgba(5,150,105,0.7)',
        },
        {
          type: 'bar' as const,
          label: 'Despesas',
          data: points.map((p) => p.expense),
          backgroundColor: 'rgba(220,38,38,0.7)',
        },
        {
          type: 'line' as const,
          label: 'Saldo Acumulado',
          data: points.map((p) => p.balance),
          borderColor: '#2563EB',
          backgroundColor: 'transparent',
          tension: 0.4,
          pointRadius: 4,
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
