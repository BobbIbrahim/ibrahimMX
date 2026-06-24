import { Component, inject } from '@angular/core';

import { SquadService } from '../../../../core/services/squad.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { SquadCard } from '../../components/squad-card/squad-card';

@Component({
  selector: 'app-squads-page',
  imports: [PageHeader, SquadCard],
  templateUrl: './squads-page.html',
  styleUrl: './squads-page.scss',
})
export class SquadsPage {
  private readonly squadService = inject(SquadService);

  readonly squads = this.squadService.getSquads();

  onCreateSquad(): void {
    console.log('New Squad clicked');
  }
}