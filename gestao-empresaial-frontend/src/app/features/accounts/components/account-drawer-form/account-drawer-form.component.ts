import { Component, ChangeDetectionStrategy, inject, signal, OnInit, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AccountService } from '../../services/account.service';
import { AccountType } from '../../models/account.model';
import { SupplierService } from '../../../suppliers/services/supplier.service';
import { ClientService } from '../../../clients/services/client.service';
import { CategoryService } from '../../../categories/services/category.service';

@Component({
  selector: 'app-account-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-drawer-form.component.html',
})
export class AccountDrawerFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accountService = inject(AccountService);
  private readonly supplierService = inject(SupplierService);
  private readonly clientService = inject(ClientService);
  private readonly categoryService = inject(CategoryService);

  readonly type = input.required<AccountType>();
  readonly accountId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);
  protected readonly showRecurrence = signal(false);

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

  constructor() {
    effect(() => {
      const id = this.accountId();
      if (id) {
        this.isEdit.set(true);
        this.loading.set(true);
        this.accountService.getById(id).subscribe({
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
      } else {
        this.isEdit.set(false);
        this.form.reset({ description: '', amount: 0, dueDate: '', categoryId: '', supplierId: '', clientId: '', notes: '' });
        this.showRecurrence.set(false);
        this.error.set(null);
      }
    });
  }

  ngOnInit() {
    this.supplierService.loadSuppliers().subscribe();
    this.clientService.loadClients().subscribe();
    this.categoryService.loadGroups().subscribe();
  }

  protected get isPayable() {
    return this.type() === 'PAYABLE';
  }

  protected toggleRecurrence() {
    this.showRecurrence.update((v) => !v);
  }

  protected cancel() {
    this.cancelled.emit();
  }

  protected save() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { description, amount, dueDate, categoryId, supplierId, clientId, notes } = this.form.getRawValue();

    if (this.isEdit()) {
      this.accountService
        .update(this.accountId()!, {
          description, amount, dueDate, categoryId,
          supplierId: supplierId || undefined,
          clientId: clientId || undefined,
          notes: notes || undefined,
        })
        .subscribe({
          next: () => { this.loading.set(false); this.saved.emit(); },
          error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar conta'); },
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
        .create({ type: this.type(), description, amount, dueDate, categoryId,
          supplierId: supplierId || undefined,
          clientId: clientId || undefined,
          notes: notes || undefined,
          recurrence,
        })
        .subscribe({
          next: () => { this.loading.set(false); this.saved.emit(); },
          error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao criar conta'); },
        });
    }
  }
}
