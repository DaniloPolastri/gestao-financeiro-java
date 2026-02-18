import { Component, ChangeDetectionStrategy, inject, signal, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { SupplierService } from '../../services/supplier.service';

@Component({
  selector: 'app-supplier-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-drawer-form.component.html',
})
export class SupplierDrawerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);

  readonly supplierId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    document: [''],
    email: [''],
    phone: [''],
  });

  constructor() {
    effect(() => {
      const id = this.supplierId();
      if (id) {
        this.isEdit.set(true);
        this.loading.set(true);
        this.supplierService.getById(id).subscribe({
          next: (s) => {
            this.form.patchValue({ name: s.name, document: s.document || '', email: s.email || '', phone: s.phone || '' });
            this.loading.set(false);
          },
          error: () => { this.error.set('Erro ao carregar fornecedor'); this.loading.set(false); },
        });
      } else {
        this.isEdit.set(false);
        this.form.reset({ name: '', document: '', email: '', phone: '' });
        this.error.set(null);
      }
    });
  }

  protected cancel() { this.cancelled.emit(); }

  protected save() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { name, document, email, phone } = this.form.getRawValue();
    const data = { name, document: document || undefined, email: email || undefined, phone: phone || undefined };
    const request$ = this.isEdit()
      ? this.supplierService.update(this.supplierId()!, data)
      : this.supplierService.create(data);
    request$.subscribe({
      next: () => { this.loading.set(false); this.saved.emit(); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar fornecedor'); },
    });
  }
}
