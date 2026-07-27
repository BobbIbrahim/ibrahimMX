import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';

import { SquadRunListItem, SquadRunOverallStatus } from '../../../../core/models/squad-run.model';
import { SquadService } from '../../../../core/services/squad.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';

@Component({
  selector: 'app-runs-dashboard-page',
  imports: [
    FormsModule,
    PageHeader,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './runs-dashboard-page.html',
  styleUrl: './runs-dashboard-page.scss',
})
export class RunsDashboardPage implements OnInit {
  private readonly squadService = inject(SquadService);
  private readonly router = inject(Router);

  readonly runs = signal<SquadRunListItem[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly statusFilter = signal<'all' | SquadRunOverallStatus>('all');

  readonly runningCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'RUNNING').length;
  });

  readonly completedCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'COMPLETED').length;
  });

  readonly failedCount = computed(() => {
    return this.runs().filter((run) => run.overallStatus === 'FAILED').length;
  });

  readonly totalCount = computed(() => this.runs().length);

  readonly filteredRuns = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();
    const selectedStatus = this.statusFilter();

    const matchingRuns = this.runs().filter((run) => {
      const matchesSearch =
        !query ||
        run.squadName.toLowerCase().includes(query) ||
        run.squadRunId.toLowerCase().includes(query);

      const matchesStatus =
        selectedStatus === 'all' || run.overallStatus === selectedStatus;

      return matchesSearch && matchesStatus;
    });

    return [...matchingRuns].sort((left, right) => {
      return new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime();
    });
  });

  ngOnInit(): void {
    this.loadRuns();
  }

  refresh(): void {
    this.loadRuns();
  }

  openRun(run: SquadRunListItem): void {
    void this.router.navigate(['/squads/live-run', run.squadId], {
      queryParams: { runId: run.squadRunId },
    });
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.statusFilter.set('all');
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

  private loadRuns(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    this.squadService.getSquadRuns().subscribe({
      next: (runs) => {
        this.runs.set(runs);
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
