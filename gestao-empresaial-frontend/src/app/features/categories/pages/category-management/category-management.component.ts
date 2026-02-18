import { Component, ChangeDetectionStrategy, inject, signal, OnInit, computed } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CategoryService } from '../../services/category.service';
import { DrawerComponent } from '../../../../shared/ui/drawer/drawer.component';
import { CategoryDrawerFormComponent } from '../../components/category-drawer-form/category-drawer-form.component';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule, DrawerComponent, CategoryDrawerFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  protected readonly categoryService = inject(CategoryService);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly addingCategoryToGroup = signal<string | null>(null);
  protected readonly newCategoryName = signal('');
  protected readonly drawerOpen = signal(false);
  protected readonly editingGroupId = signal<string | null>(null);
  protected readonly drawerTitle = computed(() => this.editingGroupId() ? 'Editar Categoria' : 'Nova Categoria');

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.categoryService.loadGroups().subscribe({
      next: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao carregar categorias'); },
    });
  }

  protected openNew() { this.editingGroupId.set(null); this.drawerOpen.set(true); }
  protected openEdit(groupId: string) { this.editingGroupId.set(groupId); this.drawerOpen.set(true); }
  protected closeDrawer() { this.drawerOpen.set(false); this.editingGroupId.set(null); }

  protected onSaved() {
    this.closeDrawer();
    this.loadData();
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
    this.newCategoryName.set('');
  }

  protected cancelAddCategory() { this.addingCategoryToGroup.set(null); }

  protected createCategory(groupId: string, name: string) {
    if (!name.trim()) return;
    this.categoryService.createCategory({ groupId, name: name.trim() }).subscribe({
      next: () => { this.addingCategoryToGroup.set(null); this.loadData(); },
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
