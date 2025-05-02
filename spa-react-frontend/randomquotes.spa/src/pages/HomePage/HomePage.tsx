import { JSX } from "react";
import styles from './HomePage.module.css';
import { useHomePageLogic } from "./useHomePageLogic";

function HomePage(): JSX.Element {
  const {
    // quoteHooks
    quoteData,
    fetchLoading,
    fetchError,
    fetchQuote,
    // quoteLikeHooks
    isLiked,
    likeLoading,
    likeError,
    handleLikeClick,
    // wshooks
    // wsError,
    // liveLikedQuotes,
  } = useHomePageLogic();

  const likeCount = quoteData?.likes ?? 0;

  return (
    <div className={styles.homeContainer}>
      <div className={styles.quoteSection}>
        <h2>Quote of the Moment</h2>

        {fetchLoading && <p className={styles.loading}>Loading quote...</p>}
        {fetchError && <p className={styles.error}>{fetchError}</p>}
        {quoteData && !fetchLoading && !fetchError && (
          <blockquote className={styles.quoteBlock}>
            <p className={styles.quoteText}>"{quoteData.text}"</p>
            <footer className={styles.quoteAuthor}>&mdash; {quoteData.author}</footer>

          </blockquote>
        )}
        
        {likeError && <p className={`${styles.error} ${styles.likeError}`}>{likeError}</p>}

        <div className={styles.buttonGroup}>
          <button onClick={fetchQuote} disabled={fetchLoading}>
            {fetchLoading ? 'Loading...' : 'Get New Quote'}
          </button>
          {quoteData && !fetchLoading && !fetchError && (
            <button
              onClick={handleLikeClick}
              className={`${styles.likeButton} ${isLiked ? styles.likedButton : ''}`} 
              aria-pressed={isLiked}
              disabled={likeLoading}
            >
              {likeLoading
                ? '...'
                : `${isLiked ? '❤️ Liked' : '🤍 Like'} (${likeCount})`}
            </button>
          )}
        </div>
      </div>

      {/* <aside className={styles.streamSection}>
        <h3>Recently Liked Quotes</h3>
        {wsError && <p className={styles.error}>{wsError}</p>}
        {liveLikedQuotes.length === 0 && !wsError ? (
          <p>No liked quotes yet...</p>
        ) : (
          <ul className={styles.streamList}>
            {liveLikedQuotes.map((likedQuote) => (
              <li key={likedQuote.id} className={styles.streamItem}>
                <p className={styles.streamQuote}>"{likedQuote.text}"</p>
                <span className={styles.streamAuthor}>&mdash; {likedQuote.author}</span>
              </li>
            ))}
          </ul>
        )}
        {!wsError && liveLikedQuotes.length > 0 && <p className={styles.streamInfo}>Live updates via WebSocket</p>}
      </aside> */}
    </div>
  );
}

export default HomePage;
