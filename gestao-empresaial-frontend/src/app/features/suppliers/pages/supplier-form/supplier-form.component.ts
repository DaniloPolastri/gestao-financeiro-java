import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SupplierService } from '../../services/supplier.service';

@Component({
  selector: 'app-supplier-form',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-form.component.html',
})
export class SupplierFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);
  private supplierId: string | null = null;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    document: [''],
    email: [''],
    phone: [''],
  });

  ngOnInit() {
    this.supplierId = this.route.snapshot.paramMap.get('id');
    if (this.supplierId) {
      this.isEdit.set(true);
      this.loading.set(true);
      this.supplierService.getById(this.supplierId).subscribe({
        next: (supplier) => {
          this.form.patchValue({
            name: supplier.name,
            document: supplier.document || '',
            email: supplier.email || '',
            phone: supplier.phone || '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Erro ao carregar fornecedor');
          this.loading.set(false);
        },
      });
    }
  }

  protected save() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { name, document, email, phone } = this.form.getRawValue();
    const data = {
      name,
      document: document || undefined,
      email: email || undefined,
      phone: phone || undefined,
    };

    const request$ = this.isEdit()
      ? this.supplierService.update(this.supplierId!, data)
      : this.supplierService.create(data);

    request$.subscribe({
      next: () => this.router.navigate(['/fornecedores']),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao salvar fornecedor');
      },
    });
  }
}
