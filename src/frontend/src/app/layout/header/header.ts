import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HeaderStateService } from './header-state.service';

@Component({
  selector: 'app-header',
  imports: [RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  readonly headerState = inject(HeaderStateService).state;
}
