import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { CompanyService } from '../services/company.service';

export const companyGuard: CanActivateFn = () => {
  const companyService = inject(CompanyService);
  const router = inject(Router);

  if (companyService.isLoaded()) {
    if (companyService.hasCompanies()) {
      return true;
    }
    return router.createUrlTree(['/empresas/nova']);
  }

  return companyService.loadCompanies$().pipe(
    map((companies) => {
      if (companies.length === 0) {
        return router.createUrlTree(['/empresas/nova']);
      }
      return true;
    }),
  );
};
