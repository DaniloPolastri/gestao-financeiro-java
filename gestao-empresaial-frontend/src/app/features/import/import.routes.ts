import { Routes } from '@angular/router';

export const importRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/bank-import-list/bank-import-list.component').then(
        (m) => m.BankImportListComponent,
      ),
  },
  {
    path: 'nova',
    loadComponent: () =>
      import('./pages/bank-import-upload/bank-import-upload.component').then(
        (m) => m.BankImportUploadComponent,
      ),
  },
  {
    path: ':id/revisao',
    loadComponent: () =>
      import('./pages/bank-import-review/bank-import-review.component').then(
        (m) => m.BankImportReviewComponent,
      ),
  },
];
