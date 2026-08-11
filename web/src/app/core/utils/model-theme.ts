/**
 * Maps an agent's runtime model/LLM name to a brand-consistent color theme.
 *
 * Purpose:
 * Give agent cards a modern, professional look where the color language
 * communicates which model family powers the agent (Murex MXAgents,
 * OpenAI GPT, Anthropic Claude, Google Gemini, etc.) instead of an
 * arbitrary per-agent color.
 */

export interface ModelTheme {
  /** Short id used for testing/debugging. */
  id: string;
  /** Solid brand color, used for text/icons/borders. */
  primary: string;
  /** Secondary brand color, used for gradients and hover states. */
  secondary: string;
  /** Diagonal gradient combining primary + secondary, for avatars and accents. */
  gradient: string;
  /** Low-opacity tint of the primary color, for chip/pill backgrounds. */
  soft: string;
}

const MODEL_THEMES: Record<string, ModelTheme> = {
  mxagents: {
    id: 'mxagents',
    primary: '#0891b2',
    secondary: '#22d3ee',
    gradient: 'linear-gradient(135deg, #22d3ee, #0891b2)',
    soft: 'rgba(8, 145, 178, 0.16)',
  },
  gpt: {
    id: 'gpt',
    primary: '#10a37f',
    secondary: '#34d399',
    gradient: 'linear-gradient(135deg, #34d399, #0d8a6c)',
    soft: 'rgba(16, 163, 127, 0.16)',
  },
  claude: {
    id: 'claude',
    primary: '#da7756',
    secondary: '#f0a482',
    gradient: 'linear-gradient(135deg, #f0a482, #c1633f)',
    soft: 'rgba(218, 119, 86, 0.18)',
  },
  gemini: {
    id: 'gemini',
    primary: '#8b5cf6',
    secondary: '#4c8dff',
    gradient: 'linear-gradient(135deg, #60a5fa, #a855f7)',
    soft: 'rgba(139, 92, 246, 0.18)',
  },
  default: {
    id: 'default',
    primary: 'var(--color-accent)',
    secondary: 'var(--color-accent-strong)',
    gradient: 'linear-gradient(135deg, var(--color-accent), var(--color-accent-strong))',
    soft: 'color-mix(in srgb, var(--color-accent) 14%, transparent)',
  },
};

/** Matches a model/LLM display name (e.g. "GPT-4.1", "Claude Sonnet") to its brand theme. */
export function getModelTheme(model: string | null | undefined): ModelTheme {
  const key = (model ?? '').toLowerCase();

  if (key.includes('mxagent')) {
    return MODEL_THEMES['mxagents'];
  }

  if (key.includes('gpt') || key.includes('openai')) {
    return MODEL_THEMES['gpt'];
  }

  if (key.includes('claude') || key.includes('anthropic')) {
    return MODEL_THEMES['claude'];
  }

  if (key.includes('gemini') || key.includes('google')) {
    return MODEL_THEMES['gemini'];
  }

  return MODEL_THEMES['default'];
}
