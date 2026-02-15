import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/auth/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50">
      <div class="w-full max-w-md p-8 bg-white rounded-lg border border-gray-200">
        <h1 class="text-2xl font-bold text-gray-900 mb-2">Criar conta</h1>
        <p class="text-sm text-gray-500 mb-6">Comece a usar o FinDash gratuitamente</p>

        @if (error()) {
          <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
            {{ error() }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Nome</label>
            <input
              formControlName="name"
              type="text"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Seu nome completo"
            />
          </div>

          <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              formControlName="email"
              type="email"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="seu@email.com"
            />
          </div>

          <div class="mb-6">
            <label class="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input
              formControlName="password"
              type="password"
              class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Minimo 6 caracteres"
            />
          </div>

          <button
            type="submit"
            [disabled]="form.invalid || loading()"
            class="w-full py-2 px-4 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading() ? 'Criando conta...' : 'Criar conta' }}
          </button>
        </form>

        <p class="mt-4 text-center text-sm text-gray-500">
          Ja tem conta?
          <a routerLink="/login" class="text-blue-600 hover:text-blue-700 font-medium">
            Entrar
          </a>
        </p>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  protected submit() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { name, email, password } = this.form.getRawValue();
    this.authService.register(name, email, password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao criar conta');
      },
    });
  }
}
