import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ThemeService } from '../../services/theme.service';

interface NavigationItem {
  route: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
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

  /** Mobile drawer visibility (unchanged behaviour: closed by default, opened via the hamburger). */
  private readonly navOpen = signal(false);

  /** Desktop persistent-sidebar collapse state (expanded by default, matching prior behaviour). */
  private readonly sidebarCollapsed = signal(false);

  readonly isNavOpen = this.navOpen.asReadonly();
  readonly isSidebarCollapsed = this.sidebarCollapsed.asReadonly();

  toggleNav(): void {
    this.navOpen.update((isOpen) => !isOpen);
  }

  closeNav(): void {
    this.navOpen.set(false);
  }

  /** Collapses/restores the persistent left navigation between a full panel and a slim icon rail. */
  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  get sidebarToggleLabel(): string {
    return this.isSidebarCollapsed() ? 'Expand navigation menu' : 'Collapse navigation menu';
  }
}
