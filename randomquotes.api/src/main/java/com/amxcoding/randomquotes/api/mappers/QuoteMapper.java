package com.amxcoding.randomquotes.api.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.api.models.QuoteResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuoteMapper {
    // QuoteResponse
    QuoteResponse toQuoteResponse(Quote quote);
    List<QuoteResponse> toQuoteResponseList(List<Quote> quotes);


}
