import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-16 bg-white border-b border-gray-200' },
  template: `
    <div class="flex items-center justify-between h-full px-6">
      <div></div>
      <div class="flex items-center gap-4">
        <button class="flex items-center gap-2 px-3 py-1.5 text-sm border border-gray-200 rounded-md hover:bg-gray-50">
          <i class="pi pi-building text-gray-500"></i>
          <span class="text-gray-700">Selecionar empresa</span>
          <i class="pi pi-chevron-down text-xs text-gray-400"></i>
        </button>
        <button class="flex items-center gap-2 px-3 py-1.5 text-sm rounded-md hover:bg-gray-50">
          <i class="pi pi-user text-gray-500"></i>
          <span class="text-gray-700">Perfil</span>
        </button>
      </div>
    </div>
  `,
})
export class HeaderComponent {}
