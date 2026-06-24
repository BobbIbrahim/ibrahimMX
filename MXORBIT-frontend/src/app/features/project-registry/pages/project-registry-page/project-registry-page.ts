import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Project, ProjectStatus } from '../../../../core/models/project.model';
import { ProjectService } from '../../../../core/services/project.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { ProjectCard } from '../../components/project-card/project-card';

type ProjectStatusFilter = ProjectStatus | 'all';

type ProjectSortOption =
  | 'name-asc'
  | 'status'
  | 'updated-desc'
  | 'agents-desc';

@Component({
  selector: 'app-project-registry-page',
  imports: [
    PageHeader,
    ProjectCard,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './project-registry-page.html',
  styleUrl: './project-registry-page.scss',
})
export class ProjectRegistryPage {
  private readonly projectService = inject(ProjectService);

  readonly projects = this.projectService.getProjects();

  readonly searchTerm = signal('');
  readonly selectedStatus = signal<ProjectStatusFilter>('all');
  readonly selectedSort = signal<ProjectSortOption>('name-asc');

  readonly filteredProjects = computed(() => {
    const searchTerm = this.searchTerm().trim().toLowerCase();
    const selectedStatus = this.selectedStatus();

    const filteredProjects = this.projects().filter((project) => {
      const matchesStatus =
        selectedStatus === 'all' || project.status === selectedStatus;

      const searchableText = [
        project.name,
        project.description,
        project.status,
        project.repositoryUrl,
        project.defaultBranch,
        ...project.stack,
      ]
        .join(' ')
        .toLowerCase();

      const matchesSearch =
        searchTerm.length === 0 || searchableText.includes(searchTerm);

      return matchesStatus && matchesSearch;
    });

    return this.sortProjects(filteredProjects, this.selectedSort());
  });

  readonly totalProjects = computed(() => this.projects().length);

  readonly activeProjects = computed(
    () => this.projects().filter((project) => project.status === 'active').length,
  );

  readonly pausedProjects = computed(
    () => this.projects().filter((project) => project.status === 'paused').length,
  );

  readonly completedProjects = computed(
    () =>
      this.projects().filter((project) => project.status === 'completed').length,
  );

  readonly totalAssignedAgents = computed(() =>
    this.projects().reduce((sum, project) => sum + project.agentIds.length, 0),
  );

  readonly hasActiveFilters = computed(
    () =>
      this.searchTerm().trim().length > 0 ||
      this.selectedStatus() !== 'all' ||
      this.selectedSort() !== 'name-asc',
  );

  setSearchTerm(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  setStatusFilter(status: ProjectStatusFilter): void {
    this.selectedStatus.set(status);
  }

  setSortOption(sortOption: ProjectSortOption): void {
    this.selectedSort.set(sortOption);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedStatus.set('all');
    this.selectedSort.set('name-asc');
  }

  private sortProjects(
    projects: Project[],
    sortOption: ProjectSortOption,
  ): Project[] {
    const projectsToSort = [...projects];

    if (sortOption === 'name-asc') {
      return projectsToSort.sort((firstProject, secondProject) =>
        firstProject.name.localeCompare(secondProject.name),
      );
    }

    if (sortOption === 'status') {
      const statusPriority: Record<ProjectStatus, number> = {
        active: 1,
        paused: 2,
        completed: 3,
      };

      return projectsToSort.sort(
        (firstProject, secondProject) =>
          statusPriority[firstProject.status] -
          statusPriority[secondProject.status],
      );
    }

    if (sortOption === 'updated-desc') {
      return projectsToSort.sort(
        (firstProject, secondProject) =>
          new Date(secondProject.updatedAt).getTime() -
          new Date(firstProject.updatedAt).getTime(),
      );
    }

    return projectsToSort.sort(
      (firstProject, secondProject) =>
        secondProject.agentIds.length - firstProject.agentIds.length,
    );
  }
}