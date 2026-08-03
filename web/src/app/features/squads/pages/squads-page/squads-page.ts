import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { SquadService } from '../../../../core/services/squad.service';
import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { SquadCard } from '../../components/squad-card/squad-card';
import {
  SquadCreateDialog,
  SquadCreateDialogData,
  SquadCreateDialogResult,
} from '../../components/squad-create-dialog/squad-create-dialog';
import {
  SquadDeleteConfirmDialog,
  SquadDeleteConfirmDialogData,
} from '../../components/squad-delete-confirm-dialog/squad-delete-confirm-dialog';

type SquadTypeFilter = 'all' | 'hardcoded-flow' | 'prompt-squad';

type SquadSortOption = 'name-asc' | 'name-desc' | 'steps-desc' | 'edges-desc' | 'members-desc';

type SquadPageSize = 6 | 12 | 24;

type SquadViewMode = 'cards' | 'list';

const SQUAD_VIEW_MODE_STORAGE_KEY = 'mxorbit.squads.viewMode';

function getInitialSquadViewMode(): SquadViewMode {
  if (typeof window === 'undefined') {
    return 'cards';
  }

  try {
    const storedViewMode = window.localStorage.getItem(SQUAD_VIEW_MODE_STORAGE_KEY);

    return storedViewMode === 'list' || storedViewMode === 'cards' ? storedViewMode : 'cards';
  } catch (error) {
    console.warn('Failed to read the persisted squad view mode:', error);

    return 'cards';
  }
}

function persistSquadViewMode(viewMode: SquadViewMode): void {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.localStorage.setItem(SQUAD_VIEW_MODE_STORAGE_KEY, viewMode);
  } catch (error) {
    console.warn('Failed to persist the squad view mode:', error);
  }
}

@Component({
  selector: 'app-squads-page',
  imports: [
    FormsModule,
    PageHeader,
    SquadCard,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './squads-page.html',
  styleUrl: './squads-page.scss',
})
export class SquadsPage implements OnInit {
  private readonly squadService = inject(SquadService);
  private readonly squadBuilderState = inject(SquadBuilderStateService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly squads = this.squadService.getSquads();

  readonly isLoadingSquads = signal(false);
  readonly loadSquadsError = signal<string | null>(null);

  readonly searchTerm = signal('');
  readonly typeFilter = signal<SquadTypeFilter>('all');
  readonly sortOption = signal<SquadSortOption>('name-asc');
  readonly viewMode = signal<SquadViewMode>(getInitialSquadViewMode());

  readonly currentPage = signal(1);
  readonly pageSize = signal<SquadPageSize>(6);
  readonly pageSizeOptions: readonly SquadPageSize[] = [6, 12, 24];

  readonly hardcodedFlows = computed(() => {
    return this.squads().filter((squad) => squad.type === 'hardcoded-flow').length;
  });

  readonly promptSquads = computed(() => {
    return this.squads().filter((squad) => squad.type === 'prompt-squad').length;
  });

  readonly totalSteps = computed(() => {
    return this.squads().reduce((sum, squad) => sum + squad.metrics.steps, 0);
  });

  readonly totalEdges = computed(() => {
    return this.squads().reduce((sum, squad) => sum + squad.metrics.edges, 0);
  });

  readonly totalMembers = computed(() => {
    return this.squads().reduce((sum, squad) => sum + squad.metrics.members, 0);
  });

  readonly filteredSquads = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();
    const selectedType = this.typeFilter();
    const selectedSort = this.sortOption();

    const matchingSquads = this.squads().filter((squad) => {
      const matchesSearch =
        !query ||
        squad.name.toLowerCase().includes(query) ||
        squad.description.toLowerCase().includes(query) ||
        squad.type.toLowerCase().includes(query);

      const matchesType = selectedType === 'all' || squad.type === selectedType;

      return matchesSearch && matchesType;
    });

    return [...matchingSquads].sort((left, right) => {
      switch (selectedSort) {
        case 'name-desc':
          return right.name.localeCompare(left.name);

        case 'steps-desc':
          return right.metrics.steps - left.metrics.steps;

        case 'edges-desc':
          return right.metrics.edges - left.metrics.edges;

        case 'members-desc':
          return right.metrics.members - left.metrics.members;

        case 'name-asc':
        default:
          return left.name.localeCompare(right.name);
      }
    });
  });

  readonly totalPages = computed(() => {
    const filteredSquadCount = this.filteredSquads().length;

    if (filteredSquadCount === 0) {
      return 0;
    }

    return Math.ceil(filteredSquadCount / this.pageSize());
  });

  readonly effectiveCurrentPage = computed(() => {
    const availablePages = this.totalPages();

    if (availablePages === 0) {
      return 1;
    }

    return Math.min(Math.max(this.currentPage(), 1), availablePages);
  });

  readonly paginatedSquads = computed(() => {
    const page = this.effectiveCurrentPage();
    const selectedPageSize = this.pageSize();
    const startIndex = (page - 1) * selectedPageSize;
    const endIndex = startIndex + selectedPageSize;

    return this.filteredSquads().slice(startIndex, endIndex);
  });

  readonly visiblePageNumbers = computed(() => {
    return Array.from({ length: this.totalPages() }, (_, index) => index + 1);
  });

  readonly resultRangeStart = computed(() => {
    if (this.filteredSquads().length === 0) {
      return 0;
    }

    return (this.effectiveCurrentPage() - 1) * this.pageSize() + 1;
  });

  readonly resultRangeEnd = computed(() => {
    if (this.filteredSquads().length === 0) {
      return 0;
    }

    return Math.min(this.effectiveCurrentPage() * this.pageSize(), this.filteredSquads().length);
  });

  readonly hasPreviousPage = computed(() => {
    return this.totalPages() > 0 && this.effectiveCurrentPage() > 1;
  });

  readonly hasNextPage = computed(() => {
    return this.totalPages() > 0 && this.effectiveCurrentPage() < this.totalPages();
  });

  ngOnInit(): void {
    this.loadSquads();
  }

  onCreateSquad(): void {
    const dialogRef = this.dialog.open<
      SquadCreateDialog,
      SquadCreateDialogData,
      SquadCreateDialogResult
    >(SquadCreateDialog, {
      data: {},
      width: '44rem',
      maxWidth: '92vw',
      maxHeight: '90vh',
      autoFocus: false,
      restoreFocus: false,
      panelClass: 'squad-create-dialog-panel',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) {
        return;
      }

      this.squadBuilderState.createDraft(result);

      if (result.type === 'hardcoded-flow') {
        void this.router.navigate(['/squads/builder/new']);
        return;
      }

      console.log('Prompt Squad flow will be implemented later:', result);
    });
  }

  openSquad(squadId: string): void {
    this.squadService.getSquadByIdFromApi(squadId).subscribe({
      next: (squad) => {
        this.squadBuilderState.loadDraftFromApi(squad);

        void this.router.navigate(['/squads/builder', squad.id]);
      },
      error: (error) => {
        console.error('Failed to open squad:', error);
      },
    });
  }

  openSquadFromAction(squadId: string, event: Event): void {
    event.stopPropagation();
    this.openSquad(squadId);
  }

  deleteSquad(squadId: string): void {
    const squad = this.squads().find((existingSquad) => existingSquad.id === squadId);

    if (!squad) {
      return;
    }

    const dialogRef = this.dialog.open<
      SquadDeleteConfirmDialog,
      SquadDeleteConfirmDialogData,
      boolean
    >(SquadDeleteConfirmDialog, {
      data: {
        squadName: squad.name,
      },
      width: '30rem',
      maxWidth: '92vw',
      autoFocus: false,
      restoreFocus: true,
      disableClose: true,
      panelClass: 'squad-delete-confirm-dialog-panel',
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.squadService.deleteSquad(squadId);
      this.correctCurrentPage();
    });
  }

  deleteSquadFromAction(squadId: string, event: Event): void {
    event.stopPropagation();
    this.deleteSquad(squadId);
  }

  onSearchTermChange(searchTerm: string): void {
    this.searchTerm.set(searchTerm);
    this.resetPagination();
  }

  onTypeFilterChange(typeFilter: SquadTypeFilter): void {
    this.typeFilter.set(typeFilter);
    this.resetPagination();
  }

  onSortOptionChange(sortOption: SquadSortOption): void {
    this.sortOption.set(sortOption);
    this.resetPagination();
  }

  onPageSizeChange(pageSize: SquadPageSize): void {
    this.pageSize.set(pageSize);
    this.resetPagination();
  }

  onViewModeChange(viewMode: SquadViewMode): void {
    if (this.viewMode() === viewMode) {
      return;
    }

    this.viewMode.set(viewMode);
    persistSquadViewMode(viewMode);
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

  clearFilters(): void {
    this.searchTerm.set('');
    this.typeFilter.set('all');
    this.sortOption.set('name-asc');
    this.resetPagination();
  }

  getSquadTypeLabel(type: 'hardcoded-flow' | 'prompt-squad'): string {
    return type === 'hardcoded-flow' ? 'Hardcoded Flow' : 'Prompt Squad';
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

  private loadSquads(): void {
    this.isLoadingSquads.set(true);
    this.loadSquadsError.set(null);

    this.squadService.loadSquadsFromApi().subscribe({
      next: () => {
        this.isLoadingSquads.set(false);
      },
      error: (error) => {
        this.isLoadingSquads.set(false);
        this.loadSquadsError.set('Failed to load squads from the backend.');

        console.error('Failed to load squads:', error);
      },
    });
  }
}
