import { SquadType } from './squad.model';

export interface SquadTypeDescriptor {
  value: SquadType;
  label: string;
  icon: string;
  description: string;
  /** Only supported types can be created and executed today. */
  supported: boolean;
}

export const SQUAD_TYPES: readonly SquadTypeDescriptor[] = [
  {
    value: 'hardcoded-flow',
    label: 'Hardcoded Flow',
    icon: 'account_tree',
    description: 'Deterministic graph of steps, each bound to an agent, wired with routing rules.',
    supported: true,
  },
  {
    value: 'prompt-squad',
    label: 'Prompt Squad',
    icon: 'auto_awesome',
    description: 'An LLM plans and delegates work across agents at runtime.',
    supported: false,
  },
];

export const DEFAULT_SQUAD_TYPE: SquadType = 'hardcoded-flow';

export const SUPPORTED_SQUAD_TYPES: readonly SquadTypeDescriptor[] = SQUAD_TYPES.filter(
  (squadType) => squadType.supported,
);

/** The API still models a single coarse `workflow` type, so we translate at the boundary. */
const SQUAD_TYPE_BY_WIRE_TYPE: Record<string, SquadType> = {
  workflow: 'hardcoded-flow',
  'hardcoded-flow': 'hardcoded-flow',
  prompt: 'prompt-squad',
  'prompt-squad': 'prompt-squad',
};

const WIRE_TYPE_BY_SQUAD_TYPE: Record<SquadType, string> = {
  'hardcoded-flow': 'workflow',
  'prompt-squad': 'prompt',
};

export function normalizeSquadType(rawType: string | null | undefined): SquadType {
  const normalized = (rawType ?? '').trim().toLowerCase().replace(/_/g, '-');

  return SQUAD_TYPE_BY_WIRE_TYPE[normalized] ?? DEFAULT_SQUAD_TYPE;
}

export function toSquadWireType(type: SquadType): string {
  return WIRE_TYPE_BY_SQUAD_TYPE[type] ?? WIRE_TYPE_BY_SQUAD_TYPE[DEFAULT_SQUAD_TYPE];
}

export function getSquadTypeDescriptor(type: SquadType): SquadTypeDescriptor {
  return SQUAD_TYPES.find((squadType) => squadType.value === type) ?? SQUAD_TYPES[0];
}

export function getSquadTypeLabel(type: SquadType): string {
  return getSquadTypeDescriptor(type).label;
}

export function getSquadTypeIcon(type: SquadType): string {
  return getSquadTypeDescriptor(type).icon;
}
