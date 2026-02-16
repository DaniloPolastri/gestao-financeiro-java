import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CompanyService } from '../../../../core/services/company.service';

@Component({
  selector: 'app-company-settings',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './company-settings.component.html',
})
export class CompanySettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);

  protected readonly loading = signal(false);
  protected readonly success = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    cnpj: [''],
    segment: [''],
  });

  ngOnInit() {
    const company = this.companyService.activeCompany();
    if (company) {
      this.form.patchValue({
        name: company.name,
        cnpj: company.cnpj ?? '',
        segment: company.segment ?? '',
      });
    }
  }

  protected save() {
    if (this.form.invalid) return;
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.loading.set(true);
    this.error.set(null);
    this.success.set(false);

    const { name, cnpj, segment } = this.form.getRawValue();
    this.companyService
      .updateCompany(company.id, {
        name,
        cnpj: cnpj || undefined,
        segment: segment || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.loading.set(false);
          this.success.set(true);
          this.companyService.setActiveCompany(updated);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message || 'Erro ao atualizar empresa');
        },
      });
  }
}
