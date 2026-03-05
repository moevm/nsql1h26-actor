import { Component } from '@angular/core';

@Component({
  selector: 'app-search-filters',
  imports: [],
  templateUrl: './search-filters.html',
  styleUrl: './search-filters.scss',
})
export class SearchFilters {
  isAdvancedOpen = false;
  isGenresOpen = false;

  toggleAdvanced(): void {
    this.isAdvancedOpen = !this.isAdvancedOpen;
  }

  toggleGenres(): void {
    this.isGenresOpen = !this.isGenresOpen;
  }

  onReset(): void {
    this.isAdvancedOpen = false;
    this.isGenresOpen = false;
  }
}
