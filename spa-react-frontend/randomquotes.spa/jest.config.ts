// jest.config.ts
import type { JestConfigWithTsJest } from 'ts-jest';

const config: JestConfigWithTsJest = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  moduleNameMapper: {
    '^../constants/constants$': '<rootDir>/src/shared/constants/constants.ts',
    '^../errors/apiError$': '<rootDir>/src/shared/errors/apiError.ts',
    '^../settings/settings$': '<rootDir>/src/shared/settings/settings.ts',
    '^../types/(.*)$': '<rootDir>/src/shared/types/$1',
  },
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
  clearMocks: true,
};

export default config;
