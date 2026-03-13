import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HeaderStateService } from './header-state.service';
import { AuthSessionService } from '../../core/services/auth-session-service';
import { Route } from '@angular/router';

@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  readonly router = inject(Router);
  readonly headerState = inject(HeaderStateService).state;
  readonly authSessionService = inject(AuthSessionService);

  logout(): void {
    this.authSessionService.clearSession();
    this.router.navigate(['/']);
  }
  login(): void {
    this.authSessionService.clearSession();
    this.router.navigate(['/auth']);
  }
}
