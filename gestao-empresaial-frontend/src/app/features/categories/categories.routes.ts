import { Routes } from '@angular/router';

export const categoriesRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/category-management/category-management.component').then(
        (m) => m.CategoryManagementComponent,
      ),
  },
];
