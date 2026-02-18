import { Routes } from '@angular/router';

export const suppliersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/supplier-list/supplier-list.component').then((m) => m.SupplierListComponent),
  },
  {
    path: 'novo',
    loadComponent: () =>
      import('./pages/supplier-form/supplier-form.component').then((m) => m.SupplierFormComponent),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./pages/supplier-form/supplier-form.component').then((m) => m.SupplierFormComponent),
  },
];
