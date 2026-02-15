import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-header',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-16 bg-white border-b border-gray-200' },
  templateUrl: './header.component.html',
})
export class HeaderComponent {}
