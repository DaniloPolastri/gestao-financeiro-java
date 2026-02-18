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
  {
    path: 'novo',
    loadComponent: () =>
      import('../accounts/pages/account-form/account-form.component').then(
        (m) => m.AccountFormComponent,
      ),
    data: { type: 'RECEIVABLE' },
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('../accounts/pages/account-form/account-form.component').then(
        (m) => m.AccountFormComponent,
      ),
    data: { type: 'RECEIVABLE' },
  },
];
