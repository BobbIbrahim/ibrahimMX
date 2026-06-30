import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Squad } from '../../../../core/models/squad.model';

@Component({
  selector: 'app-squad-card',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './squad-card.html',
  styleUrl: './squad-card.scss',
})
export class SquadCard {
  readonly squad = input.required<Squad>();

  readonly deleteClicked = output<void>();

  deleteSquad(event: MouseEvent): void {
    event.stopPropagation();
    this.deleteClicked.emit();
  }
}