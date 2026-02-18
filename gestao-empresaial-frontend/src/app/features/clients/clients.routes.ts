import { Routes } from '@angular/router';

export const clientsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/client-list/client-list.component').then((m) => m.ClientListComponent),
  },
];
