import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClientService } from '../../services/client.service';

@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-form.component.html',
})
export class ClientFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly clientService = inject(ClientService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);
  private clientId: string | null = null;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    document: [''],
    email: [''],
    phone: [''],
  });

  ngOnInit() {
    this.clientId = this.route.snapshot.paramMap.get('id');
    if (this.clientId) {
      this.isEdit.set(true);
      this.loading.set(true);
      this.clientService.getById(this.clientId).subscribe({
        next: (client) => {
          this.form.patchValue({
            name: client.name,
            document: client.document || '',
            email: client.email || '',
            phone: client.phone || '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Erro ao carregar cliente');
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
      ? this.clientService.update(this.clientId!, data)
      : this.clientService.create(data);

    request$.subscribe({
      next: () => this.router.navigate(['/clientes']),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao salvar cliente');
      },
    });
  }
}
