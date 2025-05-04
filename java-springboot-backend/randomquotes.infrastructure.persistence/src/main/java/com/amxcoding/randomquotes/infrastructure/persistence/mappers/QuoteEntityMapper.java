package com.amxcoding.randomquotes.infrastructure.persistence.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class QuoteEntityMapper {
    public abstract QuoteEntity toQuoteEntity(Quote quote);
    public abstract Quote toQuote(QuoteEntity quoteEntity);

    @AfterMapping
    void afterMappingToQuoteEntity(Quote quote, @MappingTarget QuoteEntity entity) {
        entity.setTextAuthorHash(quote.generateTextAuthorHash());
    }
}
