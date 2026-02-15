import { Routes } from '@angular/router';
import { LayoutComponent } from './core/layout/components/layout/layout';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/components/dashboard/dashboard').then(
            (m) => m.DashboardComponent
          ),
      },
    ],
  },
];
