import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { SquadRunListItem, SquadRunOverallStatus } from '../../../../core/models/squad-run.model';
import { SquadService } from '../../../../core/services/squad.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import {
  RunDeleteConfirmDialog,
  RunDeleteConfirmDialogData,
} from '../../components/run-delete-confirm-dialog/run-delete-confirm-dialog';

type RunStatusFilter = 'all' | SquadRunOverallStatus;

type RunPageSize = 10 | 25 | 50;

type RunPageItem = number | 'ellipsis-left' | 'ellipsis-right';

type RunViewMode = 'list' | 'cards';

const RUN_VIEW_MODE_STORAGE_KEY = 'mxorbit.runs.viewMode';

function getInitialRunViewMode(): RunViewMode {
  if (typeof window === 'undefined') {
    return 'list';
  }

  try {
    const storedViewMode = window.localStorage.getItem(RUN_VIEW_MODE_STORAGE_KEY);

    return storedViewMode === 'cards' || storedViewMode === 'list' ? storedViewMode : 'list';
  } catch (error) {
    console.warn('Failed to read the persisted run view mode:', error);

    return 'list';
  }
}

function persistRunViewMode(viewMode: RunViewMode): void {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.localStorage.setItem(RUN_VIEW_MODE_STORAGE_KEY, viewMode);
  } catch (error) {
    console.warn('Failed to persist the run view mode:', error);
  }
}

@Component({
  selector: 'app-runs-dashboard-page',
  imports: [
    FormsModule,
    PageHeader,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './runs-dashboard-page.html',
  styleUrl: './runs-dashboard-page.scss',
})
export class RunsDashboardPage implements OnInit, OnDestroy {
  private readonly squadService = inject(SquadService);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private autoRefreshHandle?: number;

  readonly runs = signal<SquadRunListItem[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly deleteRunError = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly statusFilter = signal<RunStatusFilter>('all');
  readonly copiedRunId = signal<string | null>(null);
  readonly autoRefreshEnabled = signal(false);
  readonly lastUpdated = signal<Date | null>(null);
  readonly viewMode = signal<RunViewMode>(getInitialRunViewMode());

  readonly currentPage = signal(1);
  readonly pageSize = signal<RunPageSize>(10);
  readonly pageSizeOptions: readonly RunPageSize[] = [10, 25, 50];

  readonly runningCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'RUNNING').length;
  });

  readonly completedCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'COMPLETED').length;
  });

  readonly failedCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'FAILED').length;
  });

  readonly cancelledCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'CANCELLED').length;
  });

  readonly totalCount = computed(() => {
    return this.runs().length;
  });

  readonly hasActiveFilters = computed(() => {
    return this.searchTerm().trim().length > 0 || this.statusFilter() !== 'all';
  });

  readonly filteredRuns = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();
    const selectedStatus = this.statusFilter();

    const matchingRuns = this.runs().filter((run) => {
      const matchesSearch =
        !query ||
        run.squadName.toLowerCase().includes(query) ||
        run.squadRunId.toLowerCase().includes(query);

      const matchesStatus = selectedStatus === 'all' || run.overallStatus === selectedStatus;

      return matchesSearch && matchesStatus;
    });

    return [...matchingRuns].sort((left, right) => {
      return new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime();
    });
  });

  readonly totalPages = computed(() => {
    const filteredRunCount = this.filteredRuns().length;

    if (filteredRunCount === 0) {
      return 0;
    }

    return Math.ceil(filteredRunCount / this.pageSize());
  });

  readonly effectiveCurrentPage = computed(() => {
    const availablePages = this.totalPages();

    if (availablePages === 0) {
      return 1;
    }

    return Math.min(Math.max(this.currentPage(), 1), availablePages);
  });

  readonly paginatedRuns = computed(() => {
    const page = this.effectiveCurrentPage();
    const selectedPageSize = this.pageSize();
    const startIndex = (page - 1) * selectedPageSize;
    const endIndex = startIndex + selectedPageSize;

    return this.filteredRuns().slice(startIndex, endIndex);
  });

  readonly resultRangeStart = computed(() => {
    if (this.filteredRuns().length === 0) {
      return 0;
    }

    return (this.effectiveCurrentPage() - 1) * this.pageSize() + 1;
  });

  readonly resultRangeEnd = computed(() => {
    if (this.filteredRuns().length === 0) {
      return 0;
    }

    return Math.min(this.effectiveCurrentPage() * this.pageSize(), this.filteredRuns().length);
  });

  readonly hasPreviousPage = computed(() => {
    return this.totalPages() > 0 && this.effectiveCurrentPage() > 1;
  });

  readonly hasNextPage = computed(() => {
    return this.totalPages() > 0 && this.effectiveCurrentPage() < this.totalPages();
  });

  readonly visiblePageItems = computed<RunPageItem[]>(() => {
    const totalPages = this.totalPages();
    const currentPage = this.effectiveCurrentPage();

    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    if (currentPage <= 4) {
      return [1, 2, 3, 4, 5, 'ellipsis-right', totalPages];
    }

    if (currentPage >= totalPages - 3) {
      return [
        1,
        'ellipsis-left',
        totalPages - 4,
        totalPages - 3,
        totalPages - 2,
        totalPages - 1,
        totalPages,
      ];
    }

    return [
      1,
      'ellipsis-left',
      currentPage - 1,
      currentPage,
      currentPage + 1,
      'ellipsis-right',
      totalPages,
    ];
  });

  ngOnInit(): void {
    this.loadRuns();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  refresh(): void {
    this.loadRuns();
  }

  retryLoadRuns(): void {
    this.loadRuns();
  }

  viewSquads(): void {
    void this.router.navigate(['/squads']);
  }

  openRun(run: SquadRunListItem): void {
    void this.router.navigate(['/squads/live-run', run.squadId], {
      queryParams: {
        runId: run.squadRunId,
      },
    });
  }

  openRunFromAction(run: SquadRunListItem, event: Event): void {
    event.stopPropagation();
    this.openRun(run);
  }

  deleteRun(run: SquadRunListItem): void {
    const dialogRef = this.dialog.open<
      RunDeleteConfirmDialog,
      RunDeleteConfirmDialogData,
      boolean
    >(RunDeleteConfirmDialog, {
      data: {
        squadName: run.squadName,
        squadRunId: run.squadRunId,
      },
      width: '30rem',
      maxWidth: '92vw',
      autoFocus: false,
      restoreFocus: true,
      disableClose: true,
      panelClass: 'run-delete-confirm-dialog-panel',
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.deleteRunError.set(null);

      this.squadService.deleteSquadRun(run.squadRunId).subscribe({
        next: () => {
          this.removeRun(run.squadRunId);
        },
        error: (error: HttpErrorResponse) => {
          console.error('Failed to delete run:', error);

          this.deleteRunError.set(
            error.status === 409
              ? (error.error?.message ??
                  `Run ${run.squadRunId} is still in progress. Cancel it before deleting.`)
              : `Failed to delete run ${run.squadRunId}. Please try again.`,
          );
        },
      });
    });
  }

  deleteRunFromAction(run: SquadRunListItem, event: Event): void {
    event.stopPropagation();
    this.deleteRun(run);
  }

  private removeRun(squadRunId: string): void {
    this.runs.update((runs) => runs.filter((run) => run.squadRunId !== squadRunId));
    this.correctCurrentPage();
  }

  onSearchTermChange(searchTerm: string): void {
    this.searchTerm.set(searchTerm);
    this.resetPagination();
  }

  onStatusFilterChange(statusFilter: RunStatusFilter): void {
    this.statusFilter.set(statusFilter);
    this.resetPagination();
  }

  onPageSizeChange(pageSize: RunPageSize): void {
    this.pageSize.set(pageSize);
    this.resetPagination();
  }

  onViewModeChange(viewMode: RunViewMode): void {
    if (this.viewMode() === viewMode) {
      return;
    }

    this.viewMode.set(viewMode);
    persistRunViewMode(viewMode);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.statusFilter.set('all');
    this.resetPagination();
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }

    this.currentPage.set(page);
  }

  goToPreviousPage(): void {
    if (!this.hasPreviousPage()) {
      return;
    }

    this.currentPage.set(this.effectiveCurrentPage() - 1);
  }

  goToNextPage(): void {
    if (!this.hasNextPage()) {
      return;
    }

    this.currentPage.set(this.effectiveCurrentPage() + 1);
  }

  isPageNumber(pageItem: RunPageItem): pageItem is number {
    return typeof pageItem === 'number';
  }

  formatDuration(durationMs: number | null | undefined): string {
    if (durationMs === null || durationMs === undefined) {
      return '—';
    }

    const totalSeconds = Math.max(Math.floor(durationMs / 1000), 0);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }

    if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    }

    return `${seconds}s`;
  }

  formatTimestamp(timestamp: string | null | undefined): string {
    if (!timestamp) {
      return '—';
    }

    const parsed = new Date(timestamp);

    if (Number.isNaN(parsed.getTime())) {
      return '—';
    }

    return parsed.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  getStatusLabel(status: SquadRunOverallStatus | null | undefined): string {
    switch (status) {
      case 'RUNNING':
        return 'Running';

      case 'COMPLETED':
        return 'Completed';

      case 'FAILED':
        return 'Failed';

      case 'CANCELLED':
        return 'Cancelled';

      default:
        return '—';
    }
  }

  copyRunId(squadRunId: string, event: Event): void {
    event.stopPropagation();

    void navigator.clipboard.writeText(squadRunId).then(() => {
      this.copiedRunId.set(squadRunId);

      window.setTimeout(() => {
        if (this.copiedRunId() === squadRunId) {
          this.copiedRunId.set(null);
        }
      }, 1500);
    });
  }

  toggleAutoRefresh(): void {
    if (this.autoRefreshEnabled()) {
      this.stopAutoRefresh();
      this.autoRefreshEnabled.set(false);
      return;
    }

    this.autoRefreshEnabled.set(true);

    this.autoRefreshHandle = window.setInterval(() => {
      this.loadRuns();
    }, 5000);
  }

  formatLastUpdated(): string {
    const updatedAt = this.lastUpdated();

    if (!updatedAt) {
      return '—';
    }

    return updatedAt.toLocaleTimeString(undefined, {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  private resetPagination(): void {
    this.currentPage.set(1);
  }

  private correctCurrentPage(): void {
    const availablePages = this.totalPages();

    if (availablePages === 0) {
      this.currentPage.set(1);
      return;
    }

    if (this.currentPage() > availablePages) {
      this.currentPage.set(availablePages);
    }
  }

  private stopAutoRefresh(): void {
    if (this.autoRefreshHandle !== undefined) {
      window.clearInterval(this.autoRefreshHandle);
      this.autoRefreshHandle = undefined;
    }
  }

  private loadRuns(): void {
    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.loadError.set(null);

    this.squadService.getSquadRuns().subscribe({
      next: (runs) => {
        this.runs.set(runs);
        this.correctCurrentPage();
        this.lastUpdated.set(new Date());
        this.isLoading.set(false);
      },
      error: (error) => {
        this.loadError.set('Failed to load squad runs from the backend.');
        this.isLoading.set(false);

        console.error('Failed to load squad runs:', error);
      },
    });
  }
}
