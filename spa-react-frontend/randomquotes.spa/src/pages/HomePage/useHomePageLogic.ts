import { useCallback, useEffect, useState } from 'react';
import { QuoteData } from '../../shared/types/quoteData';
import { ApiError } from '../../shared/errors/apiError';
import { quoteService } from '../../shared/services/quoteService';
import { defaultErrorMessage } from '../../shared/constants/constants';
import { generateUniqueReactKey } from '../../shared/helpers/uniquKeyGenerator';
import { QuoteStreamData } from '../../shared/types/quoteStreamData';

export function useHomePageLogic() {
  const MAX_RECENTLY_LIKED_QUOTES = 4;

  // Quote fetch states
  const [quoteData, setQuoteData] = useState<QuoteData | null>(null);
  const [fetchLoading, setFetchLoading] = useState<boolean>(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // Quote like states
  const [isLiked, setIsLiked] = useState<boolean>(false);
  const [likeLoading, setLikeLoading] = useState<boolean>(true);
  const [likeError, setLikeError] = useState<string | null>(null);

  // SSE stream states
  const [streamLikedQuotes, setStreamLikedQuotes] = useState<QuoteStreamData[]>(
    []
  );
  const [sseError, setSSEError] = useState<string | null>(null);

  // --- Fetch hooks ---
  const fetchQuote = useCallback(async () => {
    setFetchLoading(true);
    setLikeLoading(true);
    setFetchError(null);
    setLikeError(null);
    setIsLiked(false);

    try {
      const data = await quoteService.fetchRandomQuote();

      setQuoteData(data);
      setIsLiked(data.isLiked);
    } catch (e: unknown) {
      let errorMessage = defaultErrorMessage;

      if (e instanceof ApiError) {
        errorMessage = e.message;
      }

      setFetchError(errorMessage);
      setQuoteData(null);
    } finally {
      setFetchLoading(false);
      setLikeLoading(false);
    }
  }, []);

  // executed only once
  useEffect(() => {
    fetchQuote();
  }, [fetchQuote]);

  // --- Like hooks ---
  const handleLikeClick = async () => {
    if (!quoteData) return;

    const newLikeState = !quoteData.isLiked;
    setIsLiked(newLikeState);
    setLikeLoading(true);
    setLikeError(null);

    try {
      // update quote likes
      const updatedQuote = newLikeState
        ? await quoteService.likeQuote(quoteData.id)
        : await quoteService.unlikeQuote(quoteData.id);

      setQuoteData(updatedQuote);
      setIsLiked(updatedQuote.isLiked);
    } catch (e: unknown) {
      let errorMessage = defaultErrorMessage;

      if (e instanceof ApiError) {
        errorMessage = e.message;
      }
      setLikeError(errorMessage);

      // Revert UI state if the like action fails
      setIsLiked(!newLikeState);
    } finally {
      setLikeLoading(false);
    }
  };

  // Reset likeError after x seconds if it's set
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;

    if (likeError) {
      timer = setTimeout(() => {
        setLikeError(null);
      }, 4000);
    }

    return () => clearTimeout(timer);
  }, [likeError]);

  // --- SSE Stream Hooks ---
  useEffect(() => {
    setSSEError(null); // Reset stream error on new connection attempt

    const handleStreamMessage = (likedQuote: QuoteData) => {
      // console.log('Received quote via SSE for stream list:', likedQuote);

      const newStreamEntry: QuoteStreamData = {
        reactKey: generateUniqueReactKey(),
        quote: likedQuote,
      };

      setStreamLikedQuotes((prevStream) => {
        // Prepend the new entry to show new likes on top
        const updatedStream = [newStreamEntry, ...prevStream];
        // Keep only the last N items
        return updatedStream.slice(0, MAX_RECENTLY_LIKED_QUOTES);
      });
    };

    const handleStreamError = (): // event?: Event
    void => {
      setSSEError('Connection error ...');
    };

    quoteService.connectToLikedQuotesStream(
      handleStreamMessage,
      handleStreamError
    );

    return () => {
      quoteService.disconnectLikedQuotesStream();
    };
  }, []); // Empty dependency array ensures this runs once on mount

  return {
    quoteData,
    fetchLoading,
    fetchError,
    fetchQuote,
    //
    isLiked,
    likeLoading,
    likeError,
    handleLikeClick,
    //
    sseError,
    streamLikedQuotes,
  };
}
