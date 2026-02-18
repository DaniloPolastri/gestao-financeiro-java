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
  {
    path: 'novo',
    loadComponent: () =>
      import('../accounts/pages/account-form/account-form.component').then(
        (m) => m.AccountFormComponent,
      ),
    data: { type: 'PAYABLE' },
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('../accounts/pages/account-form/account-form.component').then(
        (m) => m.AccountFormComponent,
      ),
    data: { type: 'PAYABLE' },
  },
];
