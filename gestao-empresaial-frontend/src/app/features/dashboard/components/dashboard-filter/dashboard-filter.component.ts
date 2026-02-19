import {
  ChangeDetectionStrategy,
  Component,
  computed,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DashboardPeriod } from '../../models/dashboard.model';

type Preset = 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom';

@Component({
  selector: 'app-dashboard-filter',
  templateUrl: './dashboard-filter.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
})
export class DashboardFilterComponent {
  readonly periodChange = output<DashboardPeriod>();

  protected readonly activePreset = signal<Preset>('month');
  protected readonly customFrom = signal('');
  protected readonly customTo = signal('');

  protected readonly presets: { key: Preset; label: string }[] = [
    { key: 'today', label: 'Hoje' },
    { key: 'week', label: 'Esta semana' },
    { key: 'month', label: 'Este mês' },
    { key: 'quarter', label: 'Este trimestre' },
    { key: 'year', label: 'Este ano' },
    { key: 'custom', label: 'Personalizado' },
  ];

  protected readonly isCustomValid = computed(() => {
    if (this.activePreset() !== 'custom') return true;
    return (
      !!this.customFrom() &&
      !!this.customTo() &&
      this.customFrom() <= this.customTo()
    );
  });

  selectPreset(preset: Preset): void {
    this.activePreset.set(preset);
    if (preset !== 'custom') {
      this.periodChange.emit(this.computePeriod(preset));
    }
  }

  applyCustom(): void {
    if (!this.isCustomValid()) return;
    this.periodChange.emit({ from: this.customFrom(), to: this.customTo() });
  }

  private computePeriod(preset: Preset): DashboardPeriod {
    const today = new Date();
    const fmt = (d: Date) => d.toISOString().split('T')[0];
    const from = new Date(today);

    switch (preset) {
      case 'today':
        return { from: fmt(today), to: fmt(today) };
      case 'week':
        from.setDate(today.getDate() - today.getDay());
        return { from: fmt(from), to: fmt(today) };
      case 'month':
        from.setDate(1);
        return { from: fmt(from), to: fmt(today) };
      case 'quarter':
        from.setMonth(Math.floor(today.getMonth() / 3) * 3, 1);
        return { from: fmt(from), to: fmt(today) };
      case 'year':
        from.setMonth(0, 1);
        return { from: fmt(from), to: fmt(today) };
      default:
        return { from: fmt(from), to: fmt(today) };
    }
  }
}
