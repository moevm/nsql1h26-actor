import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-admin-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './admin-login-page.html',
  styleUrl: './admin-login-page.scss',
})
export class AdminLoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.pattern(this.EMAIL_RE)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
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

    const credentials = this.loginForm.getRawValue();
    console.log('Login submit:', credentials);
  }
}
