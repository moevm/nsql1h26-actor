import { Component } from '@angular/core';
import { SearchFilters } from '../../components/search-filters/search-filters';

@Component({
  selector: 'app-search-page',
  imports: [SearchFilters],
  templateUrl: './search-page.html',
  styleUrl: './search-page.scss',
})
export class SearchPage {}
