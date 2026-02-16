import { Routes } from '@angular/router';

export const companyRoutes: Routes = [
  {
    path: 'configuracoes',
    loadComponent: () =>
      import('./pages/company-settings/company-settings.component').then(
        (m) => m.CompanySettingsComponent,
      ),
  },
  {
    path: 'configuracoes/usuarios',
    loadComponent: () =>
      import('./pages/user-management/user-management.component').then(
        (m) => m.UserManagementComponent,
      ),
  },
];
