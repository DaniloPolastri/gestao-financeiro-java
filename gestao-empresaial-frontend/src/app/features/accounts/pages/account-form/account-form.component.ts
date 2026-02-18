import { Component, ChangeDetectionStrategy, inject, signal, OnInit, computed } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AccountService } from '../../services/account.service';
import { AccountType } from '../../models/account.model';
import { SupplierService } from '../../../suppliers/services/supplier.service';
import { ClientService } from '../../../clients/services/client.service';
import { CategoryService } from '../../../categories/services/category.service';

@Component({
  selector: 'app-account-form',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-form.component.html',
})
export class AccountFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accountService = inject(AccountService);
  private readonly supplierService = inject(SupplierService);
  private readonly clientService = inject(ClientService);
  private readonly categoryService = inject(CategoryService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);
  protected readonly showRecurrence = signal(false);
  private accountId: string | null = null;

  protected readonly type: AccountType = this.route.snapshot.data['type'] ?? 'PAYABLE';
  protected readonly isPayable = computed(() => this.type === 'PAYABLE');
  protected readonly pageTitle = computed(() => {
    if (this.isEdit()) return this.isPayable() ? 'Editar Conta a Pagar' : 'Editar Conta a Receber';
    return this.isPayable() ? 'Nova Conta a Pagar' : 'Nova Conta a Receber';
  });
  protected readonly basePath = computed(() => this.isPayable() ? '/contas-a-pagar' : '/contas-a-receber');

  protected readonly suppliers = this.supplierService.suppliers;
  protected readonly clients = this.clientService.clients;
  protected readonly groups = this.categoryService.groups;

  protected readonly form = this.fb.nonNullable.group({
    description: ['', [Validators.required, Validators.minLength(2)]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    dueDate: ['', [Validators.required]],
    categoryId: ['', [Validators.required]],
    supplierId: [''],
    clientId: [''],
    notes: [''],
  });

  protected readonly recurrenceForm = this.fb.nonNullable.group({
    frequency: ['MONTHLY'],
    endDate: [''],
    maxOccurrences: [0],
  });

  ngOnInit() {
    this.supplierService.loadSuppliers().subscribe();
    this.clientService.loadClients().subscribe();
    this.categoryService.loadGroups().subscribe();

    this.accountId = this.route.snapshot.paramMap.get('id');
    if (this.accountId) {
      this.isEdit.set(true);
      this.loading.set(true);
      this.accountService.getById(this.accountId).subscribe({
        next: (account) => {
          this.form.patchValue({
            description: account.description,
            amount: account.amount,
            dueDate: account.dueDate,
            categoryId: account.category?.id || '',
            supplierId: account.supplier?.id || '',
            clientId: account.client?.id || '',
            notes: account.notes || '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Erro ao carregar conta');
          this.loading.set(false);
        },
      });
    }
  }

  protected toggleRecurrence() {
    this.showRecurrence.update((v) => !v);
  }

  protected save() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { description, amount, dueDate, categoryId, supplierId, clientId, notes } = this.form.getRawValue();

    if (this.isEdit()) {
      this.accountService
        .update(this.accountId!, {
          description,
          amount,
          dueDate,
          categoryId,
          supplierId: supplierId || undefined,
          clientId: clientId || undefined,
          notes: notes || undefined,
        })
        .subscribe({
          next: () => this.router.navigate([this.basePath()]),
          error: (err) => {
            this.loading.set(false);
            this.error.set(err.error?.message || 'Erro ao salvar conta');
          },
        });
    } else {
      const recurrence = this.showRecurrence()
        ? {
            frequency: this.recurrenceForm.getRawValue().frequency as 'MONTHLY' | 'WEEKLY' | 'BIWEEKLY' | 'YEARLY',
            endDate: this.recurrenceForm.getRawValue().endDate || undefined,
            maxOccurrences: this.recurrenceForm.getRawValue().maxOccurrences || undefined,
          }
        : undefined;

      this.accountService
        .create({
          type: this.type,
          description,
          amount,
          dueDate,
          categoryId,
          supplierId: supplierId || undefined,
          clientId: clientId || undefined,
          notes: notes || undefined,
          recurrence,
        })
        .subscribe({
          next: () => this.router.navigate([this.basePath()]),
          error: (err) => {
            this.loading.set(false);
            this.error.set(err.error?.message || 'Erro ao criar conta');
          },
        });
    }
  }
}
