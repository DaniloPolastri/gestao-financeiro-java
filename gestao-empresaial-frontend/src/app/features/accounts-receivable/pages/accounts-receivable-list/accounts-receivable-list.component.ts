import { Component, ChangeDetectionStrategy } from '@angular/core';

interface MockReceivable {
  client: string;
  date: string;
  category: string;
  status: 'Recebido' | 'Agendado' | 'Pendente';
  value: string;
}

@Component({
  selector: 'app-accounts-receivable-list',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './accounts-receivable-list.component.html',
})
export class AccountsReceivableListComponent {
  protected readonly mockData: MockReceivable[] = [
    { client: 'Tech Solutions Ltda', date: 'Hoje', category: 'Consultoria', status: 'Recebido', value: '+ R$ 4.500,00' },
    { client: 'Design Studio A', date: 'Amanha', category: 'Projeto', status: 'Agendado', value: '+ R$ 2.100,00' },
  ];
}
