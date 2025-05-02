package com.amxcoding.randomquotes.infrastructure.persistence.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class QuoteEntityMapper {

    @Mapping(target = "id", source = "id")
    public abstract QuoteEntity toQuoteEntity(Quote quote);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "likes", source = "likes")
    public abstract Quote toQuote(QuoteEntity quoteEntity);

    @AfterMapping
    void afterMappingToQuoteEntity(Quote quote, @MappingTarget QuoteEntity entity) {
        entity.setTextAuthorHash(quote.generateTextAuthorHash());
    }
}
