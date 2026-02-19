import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { BankImportService } from '../../services/bank-import.service';
import { BankImportSummary } from '../../models/bank-import.model';

@Component({
  selector: 'app-bank-import-list',
  templateUrl: './bank-import-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe],
})
export class BankImportListComponent implements OnInit {
  private readonly importService = inject(BankImportService);

  protected readonly imports = signal<BankImportSummary[]>([]);
  protected readonly loading = signal(true);

  ngOnInit(): void {
    this.importService.list().subscribe({
      next: (data) => {
        this.imports.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected statusLabel(status: string): string {
    return (
      ({ PENDING_REVIEW: 'Em Revisão', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' } as Record<
        string,
        string
      >)[status] ?? status
    );
  }

  protected statusClass(status: string): string {
    return (
      ({
        PENDING_REVIEW: 'bg-amber-100 text-amber-700',
        COMPLETED: 'bg-emerald-100 text-emerald-700',
        CANCELLED: 'bg-gray-100 text-gray-600',
      } as Record<string, string>)[status] ?? ''
    );
  }
}
