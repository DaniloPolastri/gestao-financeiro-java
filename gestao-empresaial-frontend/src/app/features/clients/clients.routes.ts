import { Routes } from '@angular/router';

export const clientsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/client-list/client-list.component').then((m) => m.ClientListComponent),
  },
  {
    path: 'novo',
    loadComponent: () =>
      import('./pages/client-form/client-form.component').then((m) => m.ClientFormComponent),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/client-form/client-form.component').then((m) => m.ClientFormComponent),
  },
];
