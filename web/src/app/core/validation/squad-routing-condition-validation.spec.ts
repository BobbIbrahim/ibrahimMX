import { describe, expect, it } from 'vitest';

import { validateSquadRoutingCondition } from './squad-routing-condition-validation';

describe('validateSquadRoutingCondition', () => {
  it('accepts equals conditions', () => {
    expect(validateSquadRoutingCondition('output.changeType equals BUG_FIX')).toBeNull();
  });

  it('accepts notEquals conditions', () => {
    expect(validateSquadRoutingCondition('output.changeType notEquals ENHANCEMENT')).toBeNull();
  });

  it('accepts in conditions with bracketed lists', () => {
    expect(validateSquadRoutingCondition('output.changeType in [BUG_FIX, HOTFIX]')).toBeNull();
  });

  it('accepts contains conditions', () => {
    expect(validateSquadRoutingCondition('output.description contains urgent')).toBeNull();
  });

  it('accepts nested output paths', () => {
    expect(
      validateSquadRoutingCondition('output.classification.changeType equals BUG_FIX'),
    ).toBeNull();
  });

  it('accepts quoted string values containing whitespace', () => {
    expect(
      validateSquadRoutingCondition('output.description equals "urgent production fix"'),
    ).toBeNull();

    expect(
      validateSquadRoutingCondition("output.description equals 'urgent production fix'"),
    ).toBeNull();
  });

  it('accepts boolean, null, integer, decimal, and negative numeric values', () => {
    expect(validateSquadRoutingCondition('output.requiresReview equals true')).toBeNull();

    expect(validateSquadRoutingCondition('output.result equals null')).toBeNull();

    expect(validateSquadRoutingCondition('output.riskScore equals 10')).toBeNull();

    expect(validateSquadRoutingCondition('output.riskScore equals 10.5')).toBeNull();

    expect(validateSquadRoutingCondition('output.riskScore equals -2')).toBeNull();
  });

  it('accepts multiple rules joined by and', () => {
    expect(
      validateSquadRoutingCondition('output.changeType equals BUG_FIX and output.test equals UNIT'),
    ).toBeNull();
  });

  it('accepts multiple rules joined by or', () => {
    expect(
      validateSquadRoutingCondition(
        'output.changeType equals BUG_FIX or output.changeType equals HOTFIX',
      ),
    ).toBeNull();
  });

  it('rejects null, undefined, empty, and blank conditions', () => {
    expect(validateSquadRoutingCondition(null)).toBe('Routing condition must not be blank.');

    expect(validateSquadRoutingCondition(undefined)).toBe('Routing condition must not be blank.');

    expect(validateSquadRoutingCondition('')).toBe('Routing condition must not be blank.');

    expect(validateSquadRoutingCondition('   ')).toBe('Routing condition must not be blank.');
  });

  it('rejects conditions containing parentheses', () => {
    expect(validateSquadRoutingCondition('(output.changeType equals BUG_FIX)')).toBe(
      'Routing condition must not contain parentheses.',
    );
  });

  it('rejects conditions that mix and with or', () => {
    expect(
      validateSquadRoutingCondition(
        'output.changeType equals BUG_FIX and output.test equals UNIT or output.priority equals HIGH',
      ),
    ).toBe("Routing condition must not mix 'and' and 'or'.");
  });

  it('rejects unsupported operators', () => {
    expect(validateSquadRoutingCondition('output.changeType startsWith BUG')).toBe(
      "Invalid routing condition rule: 'output.changeType startsWith BUG'.",
    );
  });

  it('rejects invalid output prefixes', () => {
    expect(validateSquadRoutingCondition('changeType equals BUG_FIX')).toBe(
      "Invalid routing condition rule: 'changeType equals BUG_FIX'.",
    );
  });

  it('rejects invalid output field paths', () => {
    expect(validateSquadRoutingCondition('output.classification..changeType equals BUG_FIX')).toBe(
      "Routing condition rule 'output.classification..changeType equals BUG_FIX' contains an invalid output field path.",
    );
  });

  it('rejects in conditions without bracketed lists', () => {
    expect(validateSquadRoutingCondition('output.changeType in BUG_FIX')).toBe(
      "Routing condition rule 'output.changeType in BUG_FIX' must use a bracketed list for operator 'in'.",
    );
  });

  it('rejects empty in lists', () => {
    expect(validateSquadRoutingCondition('output.changeType in []')).toBe(
      "Routing condition rule 'output.changeType in []' must not use an empty list.",
    );
  });

  it('rejects in lists containing empty values', () => {
    expect(validateSquadRoutingCondition('output.changeType in [BUG_FIX, , HOTFIX]')).toBe(
      "Routing condition rule 'output.changeType in [BUG_FIX, , HOTFIX]' contains an empty list value.",
    );
  });

  it('rejects lists used with unsupported operators', () => {
    expect(validateSquadRoutingCondition('output.changeType equals [BUG_FIX, HOTFIX]')).toBe(
      "Routing condition rule 'output.changeType equals [BUG_FIX, HOTFIX]' uses a list with an unsupported operator.",
    );
  });

  it('rejects partially quoted values', () => {
    expect(validateSquadRoutingCondition('output.description equals "urgent')).toBe(
      "Routing condition rule 'output.description equals \"urgent' contains an invalid quoted value.",
    );

    expect(validateSquadRoutingCondition("output.description equals urgent'")).toBe(
      "Routing condition rule 'output.description equals urgent'' contains an invalid quoted value.",
    );
  });

  it('rejects unquoted string values containing whitespace', () => {
    expect(validateSquadRoutingCondition('output.description equals urgent production fix')).toBe(
      "Routing condition rule 'output.description equals urgent production fix' must quote string values containing whitespace.",
    );
  });

  it('rejects trailing logical operators as invalid unquoted values', () => {
    expect(validateSquadRoutingCondition('output.changeType equals BUG_FIX and ')).toBe(
      "Routing condition rule 'output.changeType equals BUG_FIX and' must quote string values containing whitespace.",
    );

    expect(validateSquadRoutingCondition('output.changeType equals BUG_FIX or ')).toBe(
      "Routing condition rule 'output.changeType equals BUG_FIX or' must quote string values containing whitespace.",
    );
  });
});
