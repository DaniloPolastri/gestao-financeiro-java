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
  templateUrl: './sidebar.component.html',
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
