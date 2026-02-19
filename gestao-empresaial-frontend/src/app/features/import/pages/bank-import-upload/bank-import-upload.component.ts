import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { BankImportService } from '../../services/bank-import.service';

@Component({
  selector: 'app-bank-import-upload',
  templateUrl: './bank-import-upload.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankImportUploadComponent {
  private readonly router = inject(Router);
  private readonly importService = inject(BankImportService);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isDragging = signal(false);

  protected readonly maxSizeBytes = 5 * 1024 * 1024; // 5MB

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  protected onDragLeave(): void {
    this.isDragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    const file = event.dataTransfer?.files[0];
    if (file) this.processFile(file);
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.processFile(file);
  }

  private processFile(file: File): void {
    this.error.set(null);

    if (file.size > this.maxSizeBytes) {
      this.error.set('Arquivo muito grande. O tamanho máximo é 5MB.');
      return;
    }

    const ext = file.name.toLowerCase().split('.').pop();
    if (!['ofx', 'qfx', 'csv'].includes(ext ?? '')) {
      this.error.set('Formato não suportado. Use arquivos OFX ou CSV.');
      return;
    }

    this.loading.set(true);
    this.importService.upload(file).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.router.navigate(['/importacao', result.id, 'revisao']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? 'Erro ao processar o arquivo.';
        const isCsvError = msg.includes('template') || msg.includes('CSV');
        this.error.set(msg + (isCsvError ? '__CSV_ERROR__' : ''));
      },
    });
  }

  protected get isCsvError(): boolean {
    return this.error()?.includes('__CSV_ERROR__') ?? false;
  }

  protected get errorMessage(): string {
    return this.error()?.replace('__CSV_ERROR__', '') ?? '';
  }

  protected downloadTemplate(): void {
    const content = 'data,descricao,valor,tipo\n2026-01-15,Exemplo Fornecedor,1500.00,DEBIT\n';
    const blob = new Blob([content], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'template-importacao.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
