package com.amxcoding.randomquotes.application.services;

import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteLikeRepository;
import com.amxcoding.randomquotes.application.interfaces.repositories.IQuoteRepository;
import com.amxcoding.randomquotes.application.interfaces.services.IQuoteLikeService;
import com.amxcoding.randomquotes.domain.entities.QuoteLike;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class QuoteLikeService implements IQuoteLikeService {

    private final IQuoteRepository quoteRepository;
    private final IQuoteLikeRepository quoteLikeRepository;
    private static final Logger logger = LoggerFactory.getLogger(QuoteLikeService.class);


    public QuoteLikeService(IQuoteLikeRepository quoteLikeRepository,
                            IQuoteRepository quoteRepository) {
        this.quoteLikeRepository = quoteLikeRepository;
        this.quoteRepository = quoteRepository;
    }


    @Transactional
    @Override
    public Mono<Boolean> likeQuote(String userId, Long quoteId) {
        if (userId == null || quoteId == null) {
            return Mono.error(new IllegalArgumentException("UserId and quoteId cannot be null."));
        }

        QuoteLike like = new QuoteLike(null, userId, quoteId);

        // errors will be propagated
        return quoteLikeRepository.saveQuoteLike(like)
                .flatMap(savedLike -> quoteRepository.incrementLikeCount(quoteId));
    }


    @Transactional
    @Override
    public Mono<Boolean> unlikeQuote(String userId, Long quoteId) {
        if (userId == null || quoteId == null) {
            return Mono.error(new IllegalArgumentException("UserId and quoteId cannot be null."));
        }

        return quoteLikeRepository.findByUserIdAndQuoteId(userId, quoteId)
                .flatMap(optionalLike -> {
                    if (optionalLike.isPresent()) {
                        // Like exists: Delete the like record
                        return quoteLikeRepository.deleteByUserIdAndQuoteId(userId, quoteId)
                                // After successful delete, decrement the counter & return its result
                                .then(Mono.defer(() -> quoteRepository.decrementLikeCount(quoteId)));
                    } else {
                        return Mono.just(false);
                    }
                });
    }


    @Override
    public Mono<Boolean> checkUserLike(String userId, Long quoteId) {
        if (userId == null || quoteId == null) {
            return Mono.error(new IllegalArgumentException("User ID and Quote ID cannot be null."));
        }

        return quoteLikeRepository.findByUserIdAndQuoteId(userId, quoteId)
                .map(Optional::isPresent);
    }
}
