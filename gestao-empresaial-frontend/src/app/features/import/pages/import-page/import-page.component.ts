import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-import-page',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './import-page.component.html',
})
export class ImportPageComponent {
  protected readonly recentImports = [
    { name: 'extrato_nubank_junho.ofx', date: 'Importado em 01/06/2024 as 10:30', status: 'Processado' },
  ];
}
