import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';

export type ThemeMode = 'dark' | 'light';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storageKey = 'agentforge-theme';

  private readonly currentThemeSignal = signal<ThemeMode>(this.getInitialTheme());

  readonly currentTheme = this.currentThemeSignal.asReadonly();

  readonly isDarkTheme = computed(() => this.currentTheme() === 'dark');

  readonly toggleLabel = computed(() =>
    this.isDarkTheme() ? 'Light mode' : 'Dark mode',
  );

  readonly toggleIcon = computed(() =>
    this.isDarkTheme() ? 'light_mode' : 'dark_mode',
  );

  constructor() {
    this.applyTheme(this.currentThemeSignal());
  }

  toggleTheme(): void {
    const nextTheme: ThemeMode = this.currentThemeSignal() === 'dark' ? 'light' : 'dark';
    this.setTheme(nextTheme);
  }

  setTheme(theme: ThemeMode): void {
    this.currentThemeSignal.set(theme);
    this.applyTheme(theme);
    this.saveTheme(theme);
  }

  private applyTheme(theme: ThemeMode): void {
    this.document.documentElement.setAttribute('data-theme', theme);
  }

  private getInitialTheme(): ThemeMode {
    const savedTheme = localStorage.getItem(this.storageKey);

    if (savedTheme === 'dark' || savedTheme === 'light') {
      return savedTheme;
    }

    return 'dark';
  }

  private saveTheme(theme: ThemeMode): void {
    localStorage.setItem(this.storageKey, theme);
  }
}