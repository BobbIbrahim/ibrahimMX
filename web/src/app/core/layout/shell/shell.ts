import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ThemeService } from '../../services/theme.service';

interface NavigationItem {
  route: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  readonly themeService = inject(ThemeService);

  readonly navigationItems: NavigationItem[] = [
    { route: '/agents', label: 'Agent Registry', icon: 'smart_toy' },
    { route: '/squads', label: 'Squads', icon: 'groups' },
    { route: '/runs', label: 'Runs', icon: 'monitor_heart' },
    { route: '/autopilot', label: 'Autopilot', icon: 'auto_awesome' },
  ];

  private readonly navOpen = signal(false);

  readonly isNavOpen = this.navOpen.asReadonly();

  toggleNav(): void {
    this.navOpen.update((isOpen) => !isOpen);
  }

  closeNav(): void {
    this.navOpen.set(false);
  }
}
