// Helper to generate unique keys for the stream list items
export function generateUniqueReactKey(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  // Basic fallback
  return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
}
