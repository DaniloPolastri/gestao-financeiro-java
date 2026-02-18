import { Component, ChangeDetectionStrategy, inject, signal, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-category-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-drawer-form.component.html',
})
export class CategoryDrawerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly categoryService = inject(CategoryService);

  readonly groupId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    type: ['EXPENSE' as 'REVENUE' | 'EXPENSE', [Validators.required]],
  });

  constructor() {
    effect(() => {
      const id = this.groupId();
      if (id) {
        this.isEdit.set(true);
        const group = this.categoryService.groups().find((g) => g.id === id);
        if (group) {
          this.form.patchValue({ name: group.name, type: group.type });
          this.form.get('type')?.disable();
        }
      } else {
        this.isEdit.set(false);
        this.form.reset({ name: '', type: 'EXPENSE' });
        this.form.get('type')?.enable();
        this.error.set(null);
      }
    });
  }

  protected cancel() { this.cancelled.emit(); }

  protected save() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { name, type } = this.form.getRawValue();
    const request$ = this.isEdit()
      ? this.categoryService.updateGroup(this.groupId()!, { name })
      : this.categoryService.createGroup({ name, type });
    request$.subscribe({
      next: () => { this.loading.set(false); this.saved.emit(); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar categoria'); },
    });
  }
}
