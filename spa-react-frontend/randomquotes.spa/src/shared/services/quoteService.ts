import { defaultErrorMessage } from '../constants/constants';
import { ApiError } from '../errors/apiError';
import appSettings from '../settings/settings';
import { ApiErrorResponse } from '../types/apiErrorResponse';
import { QuoteData } from '../types/quoteData';

class QuoteService {
  private readonly baseUrl = `${appSettings.apiBaseUrl}/quotes`;
  private eventSource: EventSource | null = null;
  private onMessageCallback: ((quote: QuoteData) => void) | null = null;
  private onErrorCallback: ((event: Event) => void) | null = null;

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

  /**
   * Establishes a Server-Sent Events (SSE) connection for liked quotes.
   */
  public connectToLikedQuotesStream(
    onMessage: (quote: QuoteData) => void,
    onError?: (event: Event) => void
  ): void {
    this.onMessageCallback = onMessage;
    this.onErrorCallback = onError || null;

    if (
      this.eventSource &&
      this.eventSource.readyState !== EventSource.CLOSED
    ) {
      // console.warn(
      //   'EventSource connection already open or connecting. Disconnect first to change callbacks or URL.'
      // );
      return;
    }

    const sseUrl = `${appSettings.apiSSEUrl}/quotes/likes`;
    this.eventSource = new EventSource(sseUrl, { withCredentials: true });

    this.eventSource.onopen = () => {
      console.log('SSE connection established for liked quotes.');
    };

    this.eventSource.onmessage = (event: MessageEvent) => {
      try {
        const likedQuote: QuoteData = JSON.parse(event.data);
        if (likedQuote && this.onMessageCallback) {
          this.onMessageCallback(likedQuote);
        }
      } catch {
        // console.error(
        //   'Failed to parse SSE message data:',
        //   error,
        //   'Raw data:',
        //   event.data
        // );
      }
    };

    this.eventSource.onerror = (event: Event) => {
      if (this.onErrorCallback) {
        this.onErrorCallback(event);
      }
    };
  }

  /**
   * Closes the Server-Sent Events (SSE) connection if it is open.
   */
  public disconnectLikedQuotesStream(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.onMessageCallback = null;
    this.onErrorCallback = null;
  }
}

export const quoteService = new QuoteService();
