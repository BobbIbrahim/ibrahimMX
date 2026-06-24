import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { SquadService } from '../../../../core/services/squad.service';
import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { SquadCard } from '../../components/squad-card/squad-card';
import {
  SquadCreateDialog,
  SquadCreateDialogData,
  SquadCreateDialogResult,
} from '../../components/squad-create-dialog/squad-create-dialog';

@Component({
  selector: 'app-squads-page',
  imports: [PageHeader, SquadCard, MatDialogModule],
  templateUrl: './squads-page.html',
  styleUrl: './squads-page.scss',
})
export class SquadsPage {
  private readonly squadService = inject(SquadService);
  private readonly squadBuilderState = inject(SquadBuilderStateService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly squads = this.squadService.getSquads();

  readonly hardcodedFlows = computed(
    () => this.squads().filter((squad) => squad.type === 'hardcoded-flow').length,
  );

  readonly promptSquads = computed(
    () => this.squads().filter((squad) => squad.type === 'prompt-squad').length,
  );

  readonly totalSteps = computed(() =>
    this.squads().reduce((sum, squad) => sum + squad.metrics.steps, 0),
  );

  readonly totalEdges = computed(() =>
    this.squads().reduce((sum, squad) => sum + squad.metrics.edges, 0),
  );

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
        this.router.navigate(['/squads/builder/new']);
        return;
      }

      console.log('Prompt Squad flow will be implemented later:', result);
    });
  }
}