import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly categoryService = inject(CategoryService);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly addingCategoryToGroup = signal<string | null>(null);
  protected readonly showNewGroupForm = signal(false);

  protected readonly groupForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    type: ['EXPENSE', [Validators.required]],
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

  protected toggleNewGroupForm() {
    this.showNewGroupForm.update((v) => !v);
  }

  protected createGroup() {
    if (this.groupForm.invalid) return;
    const { name, type } = this.groupForm.getRawValue();
    this.categoryService.createGroup({ name, type: type as 'REVENUE' | 'EXPENSE' }).subscribe({
      next: () => {
        this.groupForm.reset({ name: '', type: 'EXPENSE' });
        this.showNewGroupForm.set(false);
        this.loadData();
      },
      error: (err) => this.error.set(err.error?.message || 'Erro ao criar grupo'),
    });
  }

  protected deleteGroup(groupId: string) {
    if (!confirm('Excluir este grupo?')) return;
    this.categoryService.deleteGroup(groupId).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir grupo'),
    });
  }

  protected startAddCategory(groupId: string) {
    this.addingCategoryToGroup.set(groupId);
  }

  protected cancelAddCategory() {
    this.addingCategoryToGroup.set(null);
  }

  protected createCategory(groupId: string, name: string) {
    if (!name.trim()) return;
    this.categoryService.createCategory({ groupId, name: name.trim() }).subscribe({
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
