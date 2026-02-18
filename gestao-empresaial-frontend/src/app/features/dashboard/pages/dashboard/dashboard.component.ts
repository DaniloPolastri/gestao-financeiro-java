import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  protected readonly kpis = [
    { label: 'Saldo Atual', value: 'R$ 124.500', icon: 'pi pi-wallet', change: '+12%', positive: true },
    { label: 'Receitas', value: 'R$ 45.230', icon: 'pi pi-file', change: '+8.1%', positive: true },
    { label: 'Despesas', value: 'R$ 18.400', icon: 'pi pi-credit-card', change: '+2.3%', positive: false },
    { label: 'A Receber', value: 'R$ 8.150', icon: 'pi pi-clock', change: '7 dias', positive: null },
  ];

  protected readonly months = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun'];
}
