import { Routes } from '@angular/router';

export const suppliersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/supplier-list/supplier-list.component').then((m) => m.SupplierListComponent),
  },
];
