import { Routes } from '@angular/router';

export const accountsReceivableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/accounts-receivable-list/accounts-receivable-list.component').then(
        (m) => m.AccountsReceivableListComponent,
      ),
  },
];
