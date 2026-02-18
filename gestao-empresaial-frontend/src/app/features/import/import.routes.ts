import { Routes } from '@angular/router';

export const importRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/import-page/import-page.component').then((m) => m.ImportPageComponent),
  },
];
