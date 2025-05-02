import { defaultErrorMessage } from '../constants/constants';
import { ApiError } from '../errors/apiError';
import appSettings from '../settings/settings';
import { ApiErrorResponse } from '../types/apiErrorResponse';
import { QuoteData } from '../types/quoteData';

class QuoteService {
  private readonly baseUrl = `${appSettings.apiBaseUrl}/quotes`;
  // private websocket: WebSocket | null = null;
  // private onMessageCallback: ((quote: QuoteData) => void) | null = null;
  // private onErrorCallback: ((event: Event) => void) | null = null;

  public async fetchRandomQuote(): Promise<QuoteData> {
    const url = `${this.baseUrl}/random`;

    try {
      const response = await fetch(url, { credentials: 'include' });

      if (!response.ok) {
        const errorResponse: ApiErrorResponse = await response.json();
        const errorMessage =
          errorResponse.message ||
          'We encountered an issue while fetching the quote. Please try again later.';

        throw new ApiError(errorMessage);
      }

      const data: QuoteData = await response.json();
      return data;
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        throw error;
      } else {
        throw new ApiError(defaultErrorMessage);
      }
    }
  }

  private async sendQuoteLikeRequest(
    method: 'GET' | 'DELETE',
    quoteId: number
  ): Promise<QuoteData> {
    // throw new ApiError(errorMessage);

    try {
      const response = await fetch(`${this.baseUrl}/${quoteId}/like`, {
        method,
        credentials: 'include',
      });

      if (!response.ok) {
        const errorResponse: ApiErrorResponse = await response.json();
        const errorMessage =
          errorResponse.message ||
          'We encountered an issue while updating the quote. Please try again later';

        throw new ApiError(errorMessage);
      }

      const data: QuoteData = await response.json();
      return data;
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        throw error;
      } else {
        throw new ApiError(defaultErrorMessage);
      }
    }
  }

  public async likeQuote(quoteId: number): Promise<QuoteData> {
    return this.sendQuoteLikeRequest('GET', quoteId);
  }

  public async unlikeQuote(quoteId: number): Promise<QuoteData> {
    return this.sendQuoteLikeRequest('DELETE', quoteId);
  }

  // /**
  //  * Establishes or updates the WebSocket connection for liked quotes.
  //  * Stores the callbacks to be used for message and error handling.
  //  *
  //  * @param onMessage - Function to call when a new LikedQuote message is received.
  //  * @param onError - Optional function to call when a WebSocket error occurs.
  //  */
  // public connectToLikedQuotesStream(
  //   onMessage: (quote: QuoteData) => void,
  //   onError?: (event: Event) => void
  // ): void {
  //   // Store callbacks
  //   this.onMessageCallback = onMessage;
  //   this.onErrorCallback = onError || null;

  //   // Prevent multiple connections
  //   if (
  //     this.websocket &&
  //     (this.websocket.readyState === WebSocket.OPEN ||
  //       this.websocket.readyState === WebSocket.CONNECTING)
  //   ) {
  //     console.warn(
  //       'WebSocket connection already open or connecting. Updating callbacks.'
  //     );
  //     return; // Already connecting or open, just updated callbacks
  //   }

  //   this.websocket = new WebSocket(appSettings.apiWsUrl);
  //   this.websocket.onopen = () => {
  //     console.log('WebSocket connection established for liked quotes.');
  //   };

  //   this.websocket.onmessage = (event) => {
  //     try {
  //       const likedQuote: QuoteData = JSON.parse(event.data);

  //       if (likedQuote) {
  //         // Use the stored callback
  //         if (this.onMessageCallback) {
  //           console.log('Received liked quote via WebSocket:', likedQuote);
  //           this.onMessageCallback(likedQuote);
  //         }
  //       } else {
  //         console.warn(
  //           'Received invalid WebSocket message format:',
  //           event.data
  //         );
  //       }
  //     } catch (error) {
  //       console.error(
  //         'Failed to parse WebSocket message:',
  //         error,
  //         'Data:',
  //         event.data
  //       );
  //     }
  //   };

  //   this.websocket.onerror = (event) => {
  //     console.error('WebSocket error:', event);

  //     // Use the stored callback
  //     if (this.onErrorCallback) {
  //       this.onErrorCallback(event);
  //     }
  //   };

  //   this.websocket.onclose = (event) => {
  //     console.log(
  //       `WebSocket connection closed: Code=${event.code}, Reason=${event.reason}`
  //     );
  //     this.websocket = null;
  //   };
  // }

  // /**
  //  * Closes the WebSocket connection if it is open or connecting.
  //  */
  // public disconnectLikedQuotesStream(): void {
  //   if (
  //     this.websocket &&
  //     (this.websocket.readyState === WebSocket.OPEN ||
  //       this.websocket.readyState === WebSocket.CONNECTING)
  //   ) {
  //     console.log('Closing WebSocket connection.');
  //     this.websocket.close();
  //   }
  //   // Clear callbacks even if already closed or closing
  //   this.websocket = null; // Ensure instance is cleared
  //   this.onMessageCallback = null;
  //   this.onErrorCallback = null;
  // }
}

export const quoteService = new QuoteService();
