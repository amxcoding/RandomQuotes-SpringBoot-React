package com.amxcoding.randomquotes.api.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class QuoteMapper {

    @Mapping(target = "isLiked", ignore = true)
    public abstract QuoteResponse toQuoteResponse(Quote quote);

    public abstract Quote toQuote(QuoteResponse quoteResponse);

}
