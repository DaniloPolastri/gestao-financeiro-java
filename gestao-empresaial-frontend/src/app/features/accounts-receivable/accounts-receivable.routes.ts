import { Routes } from '@angular/router';

export const accountsReceivableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../accounts/pages/account-list/account-list.component').then(
        (m) => m.AccountListComponent,
      ),
    data: { type: 'RECEIVABLE' },
  },
];
