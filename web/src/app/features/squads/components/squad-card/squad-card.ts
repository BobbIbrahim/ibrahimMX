import { Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Squad } from '../../../../core/models/squad.model';
import { getSquadTypeDescriptor } from '../../../../core/models/squad-type';

@Component({
  selector: 'app-squad-card',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './squad-card.html',
  styleUrl: './squad-card.scss',
})
export class SquadCard {
  readonly squad = input.required<Squad>();

  readonly openClicked = output<string>();
  readonly deleteClicked = output<void>();

  readonly typeDescriptor = computed(() => getSquadTypeDescriptor(this.squad().type));

  openSquad(): void {
    this.openClicked.emit(this.squad().id);
  }

  deleteSquad(event: MouseEvent): void {
    event.stopPropagation();
    this.deleteClicked.emit();
  }
}
