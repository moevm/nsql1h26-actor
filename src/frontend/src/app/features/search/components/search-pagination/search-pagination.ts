import { Component, computed, effect, model, Input } from '@angular/core';

type PaginationItem = { type: 'page'; value: number } | { type: 'ellipsis'; key: string };

@Component({
  selector: 'app-search-pagination',
  imports: [],
  templateUrl: './search-pagination.html',
  styleUrl: './search-pagination.scss',
})
export class SearchPagination {
  @Input() pageSize: number = 12;
  actorsCount = model(0);
  currentPage = model(1);

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.actorsCount() / this.pageSize)));

  constructor() {
    effect(() => {
      const maxPage = this.totalPages();
      if (this.currentPage() > maxPage) this.currentPage.set(maxPage);
    });
  }

  readonly items = computed<PaginationItem[]>(() => {
    const total = this.totalPages();
    const current = this.currentPage();

    if (total <= 5) {
      return Array.from({ length: total }, (_, idx) => ({ type: 'page', value: idx + 1 }));
    }

    if (current <= 2) {
      return [
        { type: 'page', value: 1 },
        { type: 'page', value: 2 },
        { type: 'page', value: 3 },
        { type: 'ellipsis', key: 'right' },
        { type: 'page', value: total },
      ];
    }

    if (current >= total - 1) {
      return [
        { type: 'page', value: 1 },
        { type: 'ellipsis', key: 'left' },
        { type: 'page', value: total - 2 },
        { type: 'page', value: total - 1 },
        { type: 'page', value: total },
      ];
    }

    return [
      { type: 'page', value: 1 },
      { type: 'ellipsis', key: 'left' },
      { type: 'page', value: current - 1 },
      { type: 'page', value: current },
      { type: 'page', value: current + 1 },
      { type: 'ellipsis', key: 'right' },
      { type: 'page', value: total },
    ];
  });

  prevPage(): void {
    const nextPage = Math.max(1, this.currentPage() - 1);
    this.selectPage(nextPage);
  }

  nextPage(): void {
    const nextPage = Math.min(this.totalPages(), this.currentPage() + 1);
    this.selectPage(nextPage);
  }

  selectPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }

    if (page === this.currentPage()) {
      return;
    }

    this.currentPage.set(page);
  }
}
