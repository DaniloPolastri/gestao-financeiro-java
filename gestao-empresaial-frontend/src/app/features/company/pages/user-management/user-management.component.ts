import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import {
  CompanyService,
  CompanyMemberResponse,
} from '../../../../core/services/company.service';

@Component({
  selector: 'app-user-management',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './user-management.component.html',
})
export class UserManagementComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);

  protected readonly members = signal<CompanyMemberResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly inviteLoading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly inviteSuccess = signal(false);

  protected readonly inviteForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['EDITOR', [Validators.required]],
  });

  ngOnInit() {
    this.loadMembers();
  }

  protected inviteMember() {
    if (this.inviteForm.invalid) return;
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.inviteLoading.set(true);
    this.error.set(null);
    this.inviteSuccess.set(false);

    const { email, role } = this.inviteForm.getRawValue();
    this.companyService.inviteMember(company.id, { email, role }).subscribe({
      next: () => {
        this.inviteLoading.set(false);
        this.inviteSuccess.set(true);
        this.inviteForm.reset({ email: '', role: 'EDITOR' });
        this.loadMembers();
      },
      error: (err) => {
        this.inviteLoading.set(false);
        this.error.set(err.error?.message || 'Erro ao convidar membro');
      },
    });
  }

  protected changeRole(userId: string, newRole: string) {
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.companyService.updateMemberRole(company.id, userId, newRole).subscribe({
      next: () => this.loadMembers(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao alterar role'),
    });
  }

  protected removeMember(userId: string) {
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.companyService.removeMember(company.id, userId).subscribe({
      next: () => this.loadMembers(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao remover membro'),
    });
  }

  private loadMembers() {
    const company = this.companyService.activeCompany();
    if (!company) return;

    this.loading.set(true);
    this.companyService.getMembers(company.id).subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
