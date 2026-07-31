import { TestBed } from '@angular/core/testing';

import { AgentService } from './agent.service';

describe('AgentService', () => {
  let service: AgentService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AgentService);
  });

  it('exposes the change-classifier agent with its required input and outputs', () => {
    const agent = service.getAgentByKey('change-classifier');

    expect(agent).toBeTruthy();
    expect(agent?.inputs).toEqual(['change']);
    expect(agent?.outputs).toEqual(['change', 'changeType']);
  });

  it('exposes the test-selector agent with its required inputs and outputs', () => {
    const agent = service.getAgentByKey('test-selector');

    expect(agent).toBeTruthy();
    expect(agent?.inputs).toEqual(['change', 'changeType']);
    expect(agent?.outputs).toEqual(['change', 'changeType', 'test']);
  });

  it('exposes the deployment-planner agent with its required inputs and outputs', () => {
    const agent = service.getAgentByKey('deployment-planner');

    expect(agent).toBeTruthy();
    expect(agent?.inputs).toEqual(['change', 'changeType', 'test']);
    expect(agent?.outputs).toEqual(['change', 'changeType', 'test', 'nextAction']);
  });
});
