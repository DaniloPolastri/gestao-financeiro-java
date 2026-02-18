import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { companyGuard } from './core/guards/company.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/pages/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'empresas/nova',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/company/pages/create-company/create-company.component').then(
        (m) => m.CreateCompanyComponent,
      ),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard, companyGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.dashboardRoutes),
      },
      {
        path: '',
        loadChildren: () =>
          import('./features/company/company.routes').then((m) => m.companyRoutes),
      },
      {
        path: 'fornecedores',
        loadChildren: () =>
          import('./features/suppliers/suppliers.routes').then((m) => m.suppliersRoutes),
      },
      {
        path: 'clientes',
        loadChildren: () =>
          import('./features/clients/clients.routes').then((m) => m.clientsRoutes),
      },
      {
        path: 'categorias',
        loadChildren: () =>
          import('./features/categories/categories.routes').then((m) => m.categoriesRoutes),
      },
    ],
  },
];
