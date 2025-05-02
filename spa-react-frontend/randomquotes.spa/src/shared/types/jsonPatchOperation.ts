export interface JsonPatchOperation {
  op: 'replace' | 'add' | 'remove';
  path: string;
  value: string | number | boolean;
}
