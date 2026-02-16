import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CompanyService, CompanyResponse } from '../../core/services/company.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-16 bg-white border-b border-gray-200' },
  templateUrl: './header.component.html',
})
export class HeaderComponent {
  protected readonly companyService = inject(CompanyService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly dropdownOpen = signal(false);

  protected toggleDropdown() {
    this.dropdownOpen.update((v) => !v);
  }

  protected selectCompany(company: CompanyResponse) {
    this.companyService.setActiveCompany(company);
    this.dropdownOpen.set(false);
  }

  protected logout() {
    this.companyService.reset();
    this.authService.logout();
  }
}
