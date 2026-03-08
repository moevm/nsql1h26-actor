import { Injectable, signal } from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { DEFAULT_HEADER_STATE, HeaderState } from './header-state';

@Injectable({ providedIn: 'root' })
export class HeaderStateService {
  private readonly headerStateSignal = signal<HeaderState>(DEFAULT_HEADER_STATE);

  readonly state = this.headerStateSignal.asReadonly();

  constructor(private readonly router: Router) {
    this.syncFromRoute();

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.syncFromRoute());
  }

  private syncFromRoute(): void {
    const headerState = this.getHeaderStateFromRoute(this.router.routerState.snapshot.root);
    this.headerStateSignal.set(headerState ?? DEFAULT_HEADER_STATE);
  }

  private getHeaderStateFromRoute(root: ActivatedRouteSnapshot): HeaderState | undefined {
    let current: ActivatedRouteSnapshot | null = root;
    let headerState: HeaderState | undefined;

    while (current) {
      const routeState = current.data['header'] as HeaderState | undefined;
      if (routeState) {
        headerState = routeState;
      }
      current = current.firstChild;
    }

    return headerState;
  }
}
