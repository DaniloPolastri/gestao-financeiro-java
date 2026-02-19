import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { DashboardSummary } from '../../models/dashboard.model';

@Component({
  selector: 'app-summary-cards',
  templateUrl: './summary-cards.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe],
})
export class SummaryCardsComponent {
  readonly summary = input<DashboardSummary | null>(null);
  readonly loading = input(false);

  protected readonly cards = [
    {
      key: 'totalPayable' as const,
      label: 'A Pagar',
      icon: 'pi-arrow-up',
      color: 'text-red-600',
      bg: 'bg-red-50',
    },
    {
      key: 'totalReceivable' as const,
      label: 'A Receber',
      icon: 'pi-arrow-down',
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
    {
      key: 'totalRevenue' as const,
      label: 'Receitas',
      icon: 'pi-wallet',
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
    {
      key: 'totalExpenses' as const,
      label: 'Despesas',
      icon: 'pi-credit-card',
      color: 'text-red-600',
      bg: 'bg-red-50',
    },
  ];
}
