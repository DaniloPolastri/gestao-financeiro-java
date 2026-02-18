import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-64 min-h-screen bg-[#1E1E2D] flex flex-col' },
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  protected readonly sections: NavSection[] = [
    {
      title: 'FINANCEIRO',
      items: [
        { label: 'Dashboard', icon: 'pi pi-chart-bar', route: '/dashboard' },
        { label: 'Contas a Pagar', icon: 'pi pi-arrow-up-right', route: '/contas-a-pagar' },
        { label: 'Contas a Receber', icon: 'pi pi-arrow-down-left', route: '/contas-a-receber' },
        { label: 'Importacao OFX', icon: 'pi pi-upload', route: '/importacao' },
      ],
    },
    {
      title: 'CADASTROS',
      items: [
        { label: 'Categorias', icon: 'pi pi-tags', route: '/categorias' },
        { label: 'Fornecedores', icon: 'pi pi-building', route: '/fornecedores' },
        { label: 'Clientes', icon: 'pi pi-users', route: '/clientes' },
      ],
    },
    {
      title: 'CONFIGURACOES',
      items: [
        { label: 'Empresa', icon: 'pi pi-cog', route: '/configuracoes' },
        { label: 'Usuarios', icon: 'pi pi-user-edit', route: '/configuracoes/usuarios' },
      ],
    },
  ];

  protected get userName(): string {
    return this.authService.userName ?? 'Usuario';
  }

  protected get userEmail(): string {
    return this.authService.userEmail ?? '';
  }

  protected get userInitials(): string {
    const name = this.userName;
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }
}
