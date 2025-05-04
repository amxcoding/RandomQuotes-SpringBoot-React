package com.amxcoding.randomquotes.infrastructure.persistence.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.models.ZenQuoteEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class QuoteEntityMapper {
    public abstract ZenQuoteEntity toQuoteEntity(Quote quote);
    public abstract Quote toQuote(ZenQuoteEntity zenQuoteEntity);

    @AfterMapping
    void afterMappingToQuoteEntity(Quote quote, @MappingTarget ZenQuoteEntity entity) {
        entity.setTextAuthorHash(quote.generateTextAuthorHash());
    }
}
