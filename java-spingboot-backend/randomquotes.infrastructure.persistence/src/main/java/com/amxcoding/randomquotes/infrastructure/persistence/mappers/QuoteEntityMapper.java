package com.amxcoding.randomquotes.infrastructure.persistence.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Mapper(componentModel = "spring")
public abstract class QuoteEntityMapper {

    @Mapping(target = "id", source = "id")
    public abstract QuoteEntity toQuoteEntity(Quote domainQuote);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "likes", source = "likes")
    public abstract Quote toDomainQuote(QuoteEntity persistenceQuote);

    @AfterMapping
    protected void afterMappingToQuoteEntity(Quote domainQuote, @MappingTarget QuoteEntity entity) {
        entity.setTextAuthorHash(entity.generateTextAuthorHash(domainQuote.getAuthor(), domainQuote.getText()));
    }
}
