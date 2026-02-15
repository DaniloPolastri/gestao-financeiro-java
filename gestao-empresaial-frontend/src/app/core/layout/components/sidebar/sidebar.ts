import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-64 min-h-screen bg-gray-50 border-r border-gray-200' },
  template: `
    <div class="p-6">
      <h1 class="text-xl font-bold text-gray-900">FinDash</h1>
    </div>
    <nav class="px-3">
      @for (item of mainNav; track item.route) {
        <a
          [routerLink]="item.route"
          routerLinkActive="bg-blue-50 text-blue-700 border-blue-500"
          class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-gray-700 hover:bg-gray-100 mb-1"
        >
          <i [class]="item.icon + ' text-base'"></i>
          {{ item.label }}
        </a>
      }
      <div class="border-t border-gray-200 my-4"></div>
      @for (item of settingsNav; track item.route) {
        <a
          [routerLink]="item.route"
          routerLinkActive="bg-blue-50 text-blue-700 border-blue-500"
          class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-gray-700 hover:bg-gray-100 mb-1"
        >
          <i [class]="item.icon + ' text-base'"></i>
          {{ item.label }}
        </a>
      }
    </nav>
  `,
})
export class SidebarComponent {
  protected readonly mainNav: NavItem[] = [
    { label: 'Dashboard', icon: 'pi pi-chart-bar', route: '/dashboard' },
    { label: 'Contas a Pagar', icon: 'pi pi-arrow-up-right', route: '/contas-a-pagar' },
    { label: 'Contas a Receber', icon: 'pi pi-arrow-down-left', route: '/contas-a-receber' },
    { label: 'Importacao', icon: 'pi pi-upload', route: '/importacao' },
    { label: 'Categorias', icon: 'pi pi-tags', route: '/categorias' },
    { label: 'Fornecedores', icon: 'pi pi-building', route: '/fornecedores' },
    { label: 'Clientes', icon: 'pi pi-users', route: '/clientes' },
  ];

  protected readonly settingsNav: NavItem[] = [
    { label: 'Empresa', icon: 'pi pi-cog', route: '/configuracoes' },
    { label: 'Usuarios', icon: 'pi pi-user-edit', route: '/configuracoes/usuarios' },
  ];
}
