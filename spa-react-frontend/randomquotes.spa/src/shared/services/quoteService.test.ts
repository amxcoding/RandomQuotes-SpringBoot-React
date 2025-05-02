// import { quoteService } from './quoteService'; // Import the instance
// import { ApiError } from '../errors/apiError';
// import appSettings from '../settings/settings'; // Mocked below
// import { defaultErrorMessage } from '../constants/constants';
// import { QuoteData } from '../types/quoteData';

// // --- Mocks ---

// // Mock appSettings (adjust URLs if needed for specific tests, though usually fixed)
// jest.mock('../settings/settings', () => ({
//   apiBaseUrl: 'http://mockapi.com',
//   apiWsUrl: 'ws://mockws.com',
// }));

// // Mock the global fetch API
// const mockFetch = jest.fn();
// global.fetch = mockFetch;

// // Mock the global WebSocket API
// const mockWebSocketInstance = {
//   readyState: WebSocket.CLOSED, // Initial state
//   onopen: jest.fn(),
//   onmessage: jest.fn(),
//   onerror: jest.fn(),
//   onclose: jest.fn(),
//   close: jest.fn(),
//   send: jest.fn(), // Include send if you might use it later
// };

// // Make the mock constructor return our controllable instance
// const mockWebSocketConstructor = jest.fn(() => mockWebSocketInstance);
// global.WebSocket = mockWebSocketConstructor as any; // Cast to any to assign to global

// // Helper to reset WebSocket mock instance state between tests
// const resetWebSocketMock = () => {
//   mockWebSocketInstance.readyState = WebSocket.CLOSED;
//   // Reset all Jest mock functions associated with the instance
//   Object.values(mockWebSocketInstance).forEach((mockFn) => {
//     if (jest.isMockFunction(mockFn)) {
//       mockFn.mockClear();
//     }
//   });
//   // Also clear the constructor mock calls
//   mockWebSocketConstructor.mockClear();
// };

// // --- Tests ---

// describe('QuoteService', () => {
//   // Reset mocks and service state before each test
//   beforeEach(() => {
//     jest.clearAllMocks(); // Clears fetch, WebSocket constructor mocks
//     resetWebSocketMock(); // Resets the WS instance mock state
//     // Ensure the service's internal WS state is reset (important for singleton)
//     quoteService.disconnectLikedQuotesStream();
//   });

//   // == fetchRandomQuote Tests ==

//   describe('fetchRandomQuote', () => {
//     const mockQuote: QuoteData = {
//       id: '1',
//       content: 'Test Quote',
//       author: 'Tester',
//     };
//     const url = `${appSettings.apiBaseUrl}/quotes/random`;

//     it('should fetch a random quote successfully', async () => {
//       mockFetch.mockResolvedValueOnce({
//         ok: true,
//         json: async () => mockQuote,
//       });

//       const result = await quoteService.fetchRandomQuote();

//       expect(mockFetch).toHaveBeenCalledTimes(1);
//       expect(mockFetch).toHaveBeenCalledWith(url);
//       expect(result).toEqual(mockQuote);
//     });

//     it('should throw ApiError with API message on non-ok response', async () => {
//       const apiErrorMessage = 'Quote not found';
//       mockFetch.mockResolvedValueOnce({
//         ok: false,
//         status: 404,
//         json: async () => ({ message: apiErrorMessage }), // API error format
//       });

//       await expect(quoteService.fetchRandomQuote()).rejects.toThrow(
//         new ApiError(apiErrorMessage)
//       );
//       expect(mockFetch).toHaveBeenCalledWith(url);
//     });

//     it('should throw ApiError with default message if API message is missing', async () => {
//       mockFetch.mockResolvedValueOnce({
//         ok: false,
//         status: 500,
//         json: async () => ({}), // No 'message' field
//       });

//       await expect(quoteService.fetchRandomQuote()).rejects.toThrow(
//         new ApiError(defaultErrorMessage)
//       );
//       expect(mockFetch).toHaveBeenCalledWith(url);
//     });

//     it('should throw ApiError with default message on network error', async () => {
//       const networkError = new Error('Network failed');
//       mockFetch.mockRejectedValueOnce(networkError);

//       await expect(quoteService.fetchRandomQuote()).rejects.toThrow(
//         new ApiError(defaultErrorMessage)
//       );
//       expect(mockFetch).toHaveBeenCalledWith(url);
//     });

//     it('should throw ApiError with default message if response.json() fails', async () => {
//       mockFetch.mockResolvedValueOnce({
//         ok: false, // Or true, error can happen during json parsing too
//         status: 500,
//         json: async () => {
//           throw new Error('Invalid JSON');
//         },
//       });

//       // We expect the outer catch block to handle this and wrap in ApiError
//       await expect(quoteService.fetchRandomQuote()).rejects.toThrow(
//         new ApiError(defaultErrorMessage)
//       );
//       expect(mockFetch).toHaveBeenCalledWith(url);
//     });
//   });

//   // == WebSocket Tests ==

//   describe('WebSocket Stream', () => {
//     const mockOnMessage = jest.fn();
//     const mockOnError = jest.fn();
//     const mockQuote: QuoteData = {
//       id: 'ws1',
//       content: 'WS Quote',
//       author: 'WS Tester',
//     };

//     describe('connectToLikedQuotesStream', () => {
//       it('should create a WebSocket connection with the correct URL', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage, mockOnError);

//         expect(mockWebSocketConstructor).toHaveBeenCalledTimes(1);
//         expect(mockWebSocketConstructor).toHaveBeenCalledWith(
//           appSettings.apiWsUrl
//         );
//         expect(quoteService['websocket']).toBe(mockWebSocketInstance); // Check internal state (optional)
//       });

//       it('should store the provided callbacks', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage, mockOnError);
//         expect(quoteService['onMessageCallback']).toBe(mockOnMessage); // Access private for test
//         expect(quoteService['onErrorCallback']).toBe(mockOnError); // Access private for test
//       });

//       it('should store onMessage callback and null for onError if not provided', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage);
//         expect(quoteService['onMessageCallback']).toBe(mockOnMessage);
//         expect(quoteService['onErrorCallback']).toBeNull();
//       });

//       it('should call console.log when WebSocket opens', () => {
//         const logSpy = jest.spyOn(console, 'log').mockImplementation(() => {}); // Suppress log output
//         quoteService.connectToLikedQuotesStream(mockOnMessage);

//         // Simulate the 'open' event
//         mockWebSocketInstance.readyState = WebSocket.OPEN;
//         mockWebSocketInstance.onopen({} as Event); // Trigger manually

//         expect(logSpy).toHaveBeenCalledWith(
//           'WebSocket connection established for liked quotes.'
//         );
//         logSpy.mockRestore();
//       });

//       it('should not create a new WebSocket if already OPEN', () => {
//         const warnSpy = jest
//           .spyOn(console, 'warn')
//           .mockImplementation(() => {});
//         // Initial connection
//         quoteService.connectToLikedQuotesStream(jest.fn());
//         mockWebSocketInstance.readyState = WebSocket.OPEN; // Set state after connect call

//         // Attempt second connection
//         quoteService.connectToLikedQuotesStream(mockOnMessage, mockOnError);

//         expect(mockWebSocketConstructor).toHaveBeenCalledTimes(1); // Only called once
//         expect(warnSpy).toHaveBeenCalledWith(
//           'WebSocket connection already open or connecting. Updating callbacks.'
//         );
//         // Check if callbacks were updated
//         expect(quoteService['onMessageCallback']).toBe(mockOnMessage);
//         expect(quoteService['onErrorCallback']).toBe(mockOnError);
//         warnSpy.mockRestore();
//       });

//       it('should not create a new WebSocket if CONNECTING', () => {
//         const warnSpy = jest
//           .spyOn(console, 'warn')
//           .mockImplementation(() => {});
//         // Initial connection
//         quoteService.connectToLikedQuotesStream(jest.fn());
//         mockWebSocketInstance.readyState = WebSocket.CONNECTING; // Set state

//         // Attempt second connection
//         quoteService.connectToLikedQuotesStream(mockOnMessage);

//         expect(mockWebSocketConstructor).toHaveBeenCalledTimes(1); // Only called once
//         expect(warnSpy).toHaveBeenCalledWith(
//           'WebSocket connection already open or connecting. Updating callbacks.'
//         );
//         expect(quoteService['onMessageCallback']).toBe(mockOnMessage); // Check if callback updated
//         warnSpy.mockRestore();
//       });

//       it('should call onMessageCallback with parsed data when a valid message is received', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage);

//         // Simulate receiving a message
//         const messageEvent = {
//           data: JSON.stringify(mockQuote),
//         } as MessageEvent;
//         mockWebSocketInstance.onmessage(messageEvent); // Trigger manually

//         expect(mockOnMessage).toHaveBeenCalledTimes(1);
//         expect(mockOnMessage).toHaveBeenCalledWith(mockQuote);
//       });

//       it('should call console.error and not callback if message parsing fails', () => {
//         const errorSpy = jest
//           .spyOn(console, 'error')
//           .mockImplementation(() => {});
//         quoteService.connectToLikedQuotesStream(mockOnMessage);

//         const invalidData = 'invalid json';
//         const messageEvent = { data: invalidData } as MessageEvent;
//         mockWebSocketInstance.onmessage(messageEvent);

//         expect(mockOnMessage).not.toHaveBeenCalled();
//         expect(errorSpy).toHaveBeenCalledWith(
//           'Failed to parse WebSocket message:',
//           expect.any(Error), // Expect a SyntaxError or similar
//           'Data:',
//           invalidData
//         );
//         errorSpy.mockRestore();
//       });

//       it('should call console.warn and not callback if message data is invalid (e.g., null)', () => {
//         const warnSpy = jest
//           .spyOn(console, 'warn')
//           .mockImplementation(() => {});
//         quoteService.connectToLikedQuotesStream(mockOnMessage);

//         const invalidData = JSON.stringify(null); // Valid JSON, but falsy data
//         const messageEvent = { data: invalidData } as MessageEvent;
//         mockWebSocketInstance.onmessage(messageEvent);

//         expect(mockOnMessage).not.toHaveBeenCalled();
//         expect(warnSpy).toHaveBeenCalledWith(
//           'Received invalid WebSocket message format:',
//           invalidData
//         );
//         warnSpy.mockRestore();
//       });

//       it('should call onErrorCallback when WebSocket error occurs', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage, mockOnError);
//         const errorEvent = new Event('error');

//         // Simulate error
//         mockWebSocketInstance.onerror(errorEvent); // Trigger manually

//         expect(mockOnError).toHaveBeenCalledTimes(1);
//         expect(mockOnError).toHaveBeenCalledWith(errorEvent);
//       });

//       it('should call console.error but not throw if onErrorCallback is not provided', () => {
//         const errorSpy = jest
//           .spyOn(console, 'error')
//           .mockImplementation(() => {});
//         quoteService.connectToLikedQuotesStream(mockOnMessage); // No onError provided
//         const errorEvent = new Event('error');

//         // Simulate error - expect no crash
//         expect(() => mockWebSocketInstance.onerror(errorEvent)).not.toThrow();

//         expect(mockOnError).not.toHaveBeenCalled(); // Ensure original mock wasn't called
//         expect(errorSpy).toHaveBeenCalledWith('WebSocket error:', errorEvent);
//         errorSpy.mockRestore();
//       });

//       it('should set internal websocket to null when connection closes', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage, mockOnError);
//         expect(quoteService['websocket']).toBe(mockWebSocketInstance); // Should be connected

//         // Simulate close
//         const closeEvent = {
//           code: 1000,
//           reason: 'Normal closure',
//         } as CloseEvent;
//         mockWebSocketInstance.readyState = WebSocket.CLOSED;
//         mockWebSocketInstance.onclose(closeEvent); // Trigger manually

//         expect(quoteService['websocket']).toBeNull();
//       });
//     });

//     describe('disconnectLikedQuotesStream', () => {
//       it('should call close() on the WebSocket if OPEN', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage);
//         mockWebSocketInstance.readyState = WebSocket.OPEN; // Assume it opened

//         quoteService.disconnectLikedQuotesStream();

//         expect(mockWebSocketInstance.close).toHaveBeenCalledTimes(1);
//       });

//       it('should call close() on the WebSocket if CONNECTING', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage);
//         mockWebSocketInstance.readyState = WebSocket.CONNECTING;

//         quoteService.disconnectLikedQuotesStream();

//         expect(mockWebSocketInstance.close).toHaveBeenCalledTimes(1);
//       });

//       it('should not call close() if WebSocket is already CLOSED', () => {
//         quoteService.connectToLikedQuotesStream(mockOnMessage);
//         mockWebSocketInstance.readyState = WebSocket.CLOSED; // Already closed state
//         mockWebSocketInstance.close.mockClear(); // Clear previous calls if any

//         quoteService.disconnectLikedQuotesStream();

//         expect(mockWebSocketInstance.close).not.toHaveBeenCalled();
//       });

//       it('should not call close() if WebSocket is null', () => {
//         // Ensure WS is null initially (it should be after beforeEach)
//         expect(quoteService['websocket']).toBeNull();

//         quoteService.disconnectLikedQuotesStream();

//         expect(mockWebSocketInstance.close).not.toHaveBeenCalled(); // Instance wasn't even assigned
//       });

//       it('should set internal websocket and callbacks to null regardless of state', () => {
//         // Case 1: Connected
//         quoteService.connectToLikedQuotesStream(mockOnMessage, mockOnError);
//         mockWebSocketInstance.readyState = WebSocket.OPEN;
//         quoteService.disconnectLikedQuotesStream();
//         expect(quoteService['websocket']).toBeNull();
//         expect(quoteService['onMessageCallback']).toBeNull();
//         expect(quoteService['onErrorCallback']).toBeNull();

//         // Reset for Case 2
//         resetWebSocketMock();
//         quoteService.disconnectLikedQuotesStream(); // Clean up service state again

//         // Case 2: Initially Null
//         expect(quoteService['websocket']).toBeNull(); // Start null
//         quoteService.disconnectLikedQuotesStream(); // Disconnect when already null
//         expect(quoteService['websocket']).toBeNull();
//         expect(quoteService['onMessageCallback']).toBeNull();
//         expect(quoteService['onErrorCallback']).toBeNull();
//       });
//     });
//   });
// });
