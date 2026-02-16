import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CompanyService } from '../../../../core/services/company.service';

@Component({
  selector: 'app-create-company',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './create-company.component.html',
})
export class CreateCompanyComponent {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(255)]],
    cnpj: [''],
    segment: [''],
  });

  protected submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { name, cnpj, segment } = this.form.getRawValue();
    this.companyService
      .createCompany({
        name,
        cnpj: cnpj || undefined,
        segment: segment || undefined,
      })
      .subscribe({
        next: (company) => {
          this.companyService.addCompany(company);
          this.companyService.setActiveCompany(company);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message || 'Erro ao criar empresa');
        },
      });
  }
}
