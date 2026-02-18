import { Component, ChangeDetectionStrategy, signal } from '@angular/core';

interface MockPayable {
  supplier: string;
  dueDate: string;
  category: string;
  status: 'Pendente' | 'Atrasado' | 'Pago';
  value: string;
}

@Component({
  selector: 'app-accounts-payable-list',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './accounts-payable-list.component.html',
})
export class AccountsPayableListComponent {
  protected readonly activeTab = signal('todos');

  protected readonly tabs = [
    { key: 'todos', label: 'Todos' },
    { key: 'pendentes', label: 'Pendentes' },
    { key: 'pagos', label: 'Pagos' },
    { key: 'atrasados', label: 'Atrasados' },
  ];

  protected readonly mockData: MockPayable[] = [
    { supplier: 'AWS Cloud Services', dueDate: '15 Jun 2024', category: 'Infraestrutura', status: 'Pendente', value: '- R$ 350,00' },
    { supplier: 'Aluguel Escritorio', dueDate: '10 Jun 2024', category: 'Operacional', status: 'Atrasado', value: '- R$ 2.500,00' },
  ];

  protected setTab(tab: string) {
    this.activeTab.set(tab);
  }
}
