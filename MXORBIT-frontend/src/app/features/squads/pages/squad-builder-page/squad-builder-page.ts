import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { JsonPipe, TitleCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';

@Component({
  selector: 'app-squad-builder-page',
  imports: [RouterLink, JsonPipe, TitleCasePipe, MatButtonModule, MatIconModule],
  templateUrl: './squad-builder-page.html',
  styleUrl: './squad-builder-page.scss',
})
export class SquadBuilderPage {
  private readonly squadBuilderState = inject(SquadBuilderStateService);

  readonly draft = this.squadBuilderState.draft;
  readonly steps = this.squadBuilderState.steps;
  readonly objects = this.squadBuilderState.objects;
  readonly edges = this.squadBuilderState.edges;
  readonly selectedStep = this.squadBuilderState.selectedStep;
  readonly selectedStepId = this.squadBuilderState.selectedStepId;

  addStep(): void {
    this.squadBuilderState.addStep();
  }

  selectStep(stepId: string): void {
    this.squadBuilderState.selectStep(stepId);
  }

  clearSelection(): void {
    this.squadBuilderState.clearSelection();
  }

  saveDraft(): void {
    const payload = this.squadBuilderState.buildSavePayload();

    console.log('Squad builder save payload:', payload);
  }
}