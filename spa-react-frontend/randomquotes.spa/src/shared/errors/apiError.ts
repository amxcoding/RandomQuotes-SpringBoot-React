export class ApiError extends Error {
  public readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;

    /**
     * Ensures the prototype chain is correct
     * Else: instanceof or stack doesn't behaae as expected
     */
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}
