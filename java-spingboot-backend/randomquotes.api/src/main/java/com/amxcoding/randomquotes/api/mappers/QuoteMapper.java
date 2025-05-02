package com.amxcoding.randomquotes.api.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class QuoteMapper {

    @Mapping(target = "isLiked", ignore = true)
    public abstract QuoteResponse toQuoteResponse(Quote quote);

    public abstract Quote toQuote(QuoteResponse quoteResponse);

}
