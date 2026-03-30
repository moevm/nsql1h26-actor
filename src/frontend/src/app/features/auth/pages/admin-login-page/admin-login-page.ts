import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth-service';
import { AuthSessionService } from '../../../../core/services/auth-session-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-login-page.html',
  styleUrl: './admin-login-page.scss',
})
export class AdminLoginPage {
  private router = inject(Router);
  private readonly authApi = inject(AuthService);
  private readonly authSession = inject(AuthSessionService);

  private readonly fb = inject(FormBuilder);
  private readonly EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

  readonly loginErrorMessage = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.pattern(this.EMAIL_RE)]],
    password: ['', [Validators.required, Validators.minLength(4)]],
  });

  hasControlError(controlName: 'email' | 'password', errorKey: string): boolean {
    const control = this.loginForm.get(controlName);
    if (!control) {
      return false;
    }

    return control.hasError(errorKey) && (control.dirty || control.touched);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loginErrorMessage.set(null);
    this.isSubmitting.set(true);

    const credentials = this.loginForm.getRawValue();
    this.authApi.login(credentials).subscribe({
      next: (res) => {
        this.authSession.setSession(res);
        this.isSubmitting.set(false);
        this.loginErrorMessage.set(null);

        this.router.navigate(['/admin']);
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        if (err.status === 401) {
          this.loginErrorMessage.set('Неверный логин или пароль');
          return;
        }
        this.loginErrorMessage.set('Не удалось выполнить вход. Попробуйте позже.');
      },
    });
  }
}
