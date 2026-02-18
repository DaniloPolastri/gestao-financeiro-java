import { Routes } from '@angular/router';

export const accountsPayableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../accounts/pages/account-list/account-list.component').then(
        (m) => m.AccountListComponent,
      ),
    data: { type: 'PAYABLE' },
  },
];
