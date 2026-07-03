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

type SquadTypeFilter = 'all' | 'hardcoded-flow' | 'prompt-squad';

type SquadSortOption = 'name-asc' | 'name-desc' | 'steps-desc' | 'edges-desc' | 'members-desc';

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

  deleteSquad(squadId: string): void {
    this.squadService.deleteSquad(squadId);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.typeFilter.set('all');
    this.sortOption.set('name-asc');
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
