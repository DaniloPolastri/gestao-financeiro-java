import { Routes } from '@angular/router';

export const accountsPayableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/accounts-payable-list/accounts-payable-list.component').then(
        (m) => m.AccountsPayableListComponent,
      ),
  },
];
