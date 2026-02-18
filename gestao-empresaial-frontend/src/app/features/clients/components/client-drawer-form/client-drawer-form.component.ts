import { Component, ChangeDetectionStrategy, inject, signal, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ClientService } from '../../services/client.service';

@Component({
  selector: 'app-client-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-drawer-form.component.html',
})
export class ClientDrawerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientService = inject(ClientService);

  readonly clientId = input<string | null>(null);
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
      const id = this.clientId();
      if (id) {
        this.isEdit.set(true);
        this.loading.set(true);
        this.clientService.getById(id).subscribe({
          next: (c) => {
            this.form.patchValue({ name: c.name, document: c.document || '', email: c.email || '', phone: c.phone || '' });
            this.loading.set(false);
          },
          error: () => { this.error.set('Erro ao carregar cliente'); this.loading.set(false); },
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
      ? this.clientService.update(this.clientId()!, data)
      : this.clientService.create(data);
    request$.subscribe({
      next: () => { this.loading.set(false); this.saved.emit(); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar cliente'); },
    });
  }
}
