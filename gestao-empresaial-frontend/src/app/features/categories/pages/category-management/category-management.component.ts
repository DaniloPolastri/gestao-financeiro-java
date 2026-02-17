import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CategoryService } from '../../services/category.service';
import { CategoryGroupResponse } from '../../models/category.model';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoryService = inject(CategoryService);

  protected readonly groups = this.categoryService.groups;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly addingCategoryToGroup = signal<string | null>(null);
  protected readonly showNewGroupForm = signal(false);

  protected readonly newGroupForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    type: ['EXPENSE', [Validators.required]],
  });

  protected readonly newCategoryForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
  });

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.categoryService.loadGroups().subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao carregar categorias');
      },
    });
  }

  protected createGroup() {
    if (this.newGroupForm.invalid) return;
    const { name, type } = this.newGroupForm.getRawValue();
    this.categoryService.createGroup({ name, type: type as 'REVENUE' | 'EXPENSE' }).subscribe({
      next: () => {
        this.newGroupForm.reset({ name: '', type: 'EXPENSE' });
        this.showNewGroupForm.set(false);
        this.loadData();
      },
      error: (err) => this.error.set(err.error?.message || 'Erro ao criar grupo'),
    });
  }

  protected deleteGroup(group: CategoryGroupResponse) {
    if (!confirm(`Excluir grupo "${group.name}"?`)) return;
    this.categoryService.deleteGroup(group.id).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir grupo'),
    });
  }

  protected startAddCategory(groupId: string) {
    this.addingCategoryToGroup.set(groupId);
    this.newCategoryForm.reset({ name: '' });
  }

  protected addCategory(groupId: string) {
    if (this.newCategoryForm.invalid) return;
    const { name } = this.newCategoryForm.getRawValue();
    this.categoryService.createCategory({ groupId, name }).subscribe({
      next: () => {
        this.addingCategoryToGroup.set(null);
        this.loadData();
      },
      error: (err) => this.error.set(err.error?.message || 'Erro ao criar categoria'),
    });
  }

  protected deleteCategory(categoryId: string) {
    this.categoryService.deleteCategory(categoryId).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir categoria'),
    });
  }

  protected typeLabel(type: string): string {
    return type === 'REVENUE' ? 'Receita' : 'Despesa';
  }
}
