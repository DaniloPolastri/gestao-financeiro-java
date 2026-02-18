import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';

@Component({
  selector: 'app-drawer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'onEsc()' },
  template: `
    @if (open()) {
      <div class="fixed inset-0 bg-black/30 z-40"></div>
      <div class="fixed top-0 right-0 h-full w-[480px] bg-white z-50 flex flex-col shadow-xl">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 flex-shrink-0">
          <h2 class="text-base font-semibold text-gray-900">{{ title() }}</h2>
          <button (click)="close()" class="text-gray-400 hover:text-gray-600 transition-colors duration-150">
            <i class="pi pi-times text-sm"></i>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto px-6 py-6">
          <ng-content></ng-content>
        </div>
      </div>
    }
  `,
})
export class DrawerComponent {
  readonly open = input.required<boolean>();
  readonly title = input.required<string>();
  readonly closed = output<void>();

  protected close() {
    this.closed.emit();
  }

  protected onEsc() {
    if (this.open()) this.closed.emit();
  }
}
