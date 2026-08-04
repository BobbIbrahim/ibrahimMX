const OUTPUT_PREFIX = 'output.';

const LOGICAL_AND_PATTERN = /\s+and\s+/;
const LOGICAL_OR_PATTERN = /\s+or\s+/;

const SIMPLE_CONDITION_PATTERN =
  /^(output\.[A-Za-z][A-Za-z0-9_.]*)\s+(equals|notEquals|in|contains)\s+(.+)$/;

type RoutingConditionOperator = 'equals' | 'notEquals' | 'in' | 'contains';

export function validateSquadRoutingCondition(condition: string | null | undefined): string | null {
  if (condition === null || condition === undefined || condition.trim().length === 0) {
    return 'Routing condition must not be blank.';
  }

  const normalizedCondition = condition.trim();

  if (normalizedCondition.includes('(') || normalizedCondition.includes(')')) {
    return 'Routing condition must not contain parentheses.';
  }

  const containsAnd = LOGICAL_AND_PATTERN.test(normalizedCondition);
  const containsOr = LOGICAL_OR_PATTERN.test(normalizedCondition);

  if (containsAnd && containsOr) {
    return "Routing condition must not mix 'and' and 'or'.";
  }

  let expressions: string[];

  if (containsAnd) {
    expressions = normalizedCondition.split(LOGICAL_AND_PATTERN);
  } else if (containsOr) {
    expressions = normalizedCondition.split(LOGICAL_OR_PATTERN);
  } else {
    expressions = [normalizedCondition];
  }

  for (const expression of expressions) {
    if (!expression || expression.trim().length === 0) {
      return 'Routing condition contains an empty rule.';
    }

    const validationError = validateRule(expression.trim());

    if (validationError) {
      return validationError;
    }
  }

  if (expressions.length === 0) {
    return 'Routing condition must contain at least one rule.';
  }

  return null;
}

function validateRule(expression: string): string | null {
  const match = SIMPLE_CONDITION_PATTERN.exec(expression);

  if (!match) {
    return `Invalid routing condition rule: '${expression}'.`;
  }

  const fieldPath = match[1];
  const operator = match[2] as RoutingConditionOperator;
  const rawExpectedValue = match[3]?.trim() ?? '';

  if (!fieldPath) {
    return `Routing condition rule '${expression}' must reference an output field.`;
  }

  if (rawExpectedValue.length === 0) {
    return `Routing condition rule '${expression}' must have an expected value.`;
  }

  const outputPath = fieldPath.substring(OUTPUT_PREFIX.length);
  const outputPathError = validateOutputPath(outputPath, expression);

  if (outputPathError) {
    return outputPathError;
  }

  if (operator === 'in') {
    return validateList(rawExpectedValue, expression);
  }

  if (rawExpectedValue.startsWith('[') || rawExpectedValue.endsWith(']')) {
    return `Routing condition rule '${expression}' uses a list with an unsupported operator.`;
  }

  return validateScalar(rawExpectedValue, expression);
}

function validateOutputPath(outputPath: string, expression: string): string | null {
  if (outputPath.trim().length === 0) {
    return `Routing condition rule '${expression}' must reference an output field.`;
  }

  const pathSegments = outputPath.split('.');

  if (pathSegments.some((pathSegment) => pathSegment.trim().length === 0)) {
    return `Routing condition rule '${expression}' contains an invalid output field path.`;
  }

  return null;
}

function validateList(rawExpectedValue: string, expression: string): string | null {
  if (!rawExpectedValue.startsWith('[') || !rawExpectedValue.endsWith(']')) {
    return `Routing condition rule '${expression}' must use a bracketed list for operator 'in'.`;
  }

  const listContent = rawExpectedValue.substring(1, rawExpectedValue.length - 1).trim();

  if (listContent.length === 0) {
    return `Routing condition rule '${expression}' must not use an empty list.`;
  }

  const rawItems = listContent.split(',');

  for (const rawItem of rawItems) {
    const item = rawItem.trim();

    if (item.length === 0) {
      return `Routing condition rule '${expression}' contains an empty list value.`;
    }

    const validationError = validateScalar(item, expression);

    if (validationError) {
      return validationError;
    }
  }

  return null;
}

function validateScalar(rawValue: string, expression: string): string | null {
  const value = rawValue.trim();

  if (value.length === 0) {
    return `Routing condition rule '${expression}' must have an expected value.`;
  }

  if (isQuoted(value)) {
    return null;
  }

  if (
    value.startsWith('"') ||
    value.endsWith('"') ||
    value.startsWith("'") ||
    value.endsWith("'")
  ) {
    return `Routing condition rule '${expression}' contains an invalid quoted value.`;
  }

  if (value === 'true' || value === 'false' || value === 'null') {
    return null;
  }

  if (isNumber(value)) {
    return null;
  }

  if (containsWhitespace(value)) {
    return `Routing condition rule '${expression}' must quote string values containing whitespace.`;
  }

  return null;
}

function isQuoted(value: string): boolean {
  return (
    value.length >= 2 &&
    ((value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'")))
  );
}

function isNumber(value: string): boolean {
  return /^-?(?:0|[1-9]\d*)(?:\.\d+)?$/.test(value);
}

function containsWhitespace(value: string): boolean {
  return /\s/.test(value);
}
