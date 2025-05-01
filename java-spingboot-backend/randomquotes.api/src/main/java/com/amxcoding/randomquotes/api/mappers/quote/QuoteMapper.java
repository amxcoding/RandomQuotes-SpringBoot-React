package com.amxcoding.randomquotes.api.mappers.quote;

import com.amxcoding.randomquotes.api.models.quote.CreateQuoteRequest;
import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuoteMapper {
    // QuoteResponse
    QuoteResponse toQuoteResponse(Quote quote);
    List<QuoteResponse> toQuoteResponseList(List<Quote> quotes);

    //CreateQuoteRequest
    Quote toQuote(CreateQuoteRequest quoteRequest);

}
