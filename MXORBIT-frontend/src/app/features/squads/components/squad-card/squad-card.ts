import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { Squad } from '../../../../core/models/squad.model';

@Component({
  selector: 'app-squad-card',
  imports: [MatIconModule],
  templateUrl: './squad-card.html',
  styleUrl: './squad-card.scss',
})
export class SquadCard {
  readonly squad = input.required<Squad>();
}