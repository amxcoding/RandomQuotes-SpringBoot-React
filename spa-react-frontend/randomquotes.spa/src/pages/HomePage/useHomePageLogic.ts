import { useCallback, useEffect, useState } from 'react';
import { QuoteData } from '../../shared/types/quoteData';
import { ApiError } from '../../shared/errors/apiError';
import { quoteService } from '../../shared/services/quoteService';
import { defaultErrorMessage } from '../../shared/constants/constants';

export function useHomePageLogic() {
  // Quote fetch states
  const [quoteData, setQuoteData] = useState<QuoteData | null>(null);
  const [fetchLoading, setFetchLoading] = useState<boolean>(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // Quote like states
  const [isLiked, setIsLiked] = useState<boolean>(false);
  const [likeLoading, setLikeLoading] = useState<boolean>(true);
  const [likeError, setLikeError] = useState<string | null>(null);

  // WS stream states
  //   const [liveLikedQuotes, setLiveLikedQuotes] = useState<QuoteData[]>([]);
  //   const [wsError, setWsError] = useState<string | null>(null);

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

  //   useEffect(() => {
  //     setWsError(null);
  //     const handleWebSocketMessage = (newLikedQuote: QuoteData) => {
  //       setLiveLikedQuotes((prev) => [newLikedQuote, ...prev].slice(0, 10));
  //     };

  //     const handleWebSocketError = () => {
  //       setWsError('Connection error with the liked quotes stream.');
  //     };

  //     quoteService.connectToLikedQuotesStream(
  //       handleWebSocketMessage,
  //       handleWebSocketError
  //     );

  //     return () => {
  //       quoteService.disconnectLikedQuotesStream();
  //     };
  //   }, []);

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
    // wsError,
    // liveLikedQuotes,
  };
}
