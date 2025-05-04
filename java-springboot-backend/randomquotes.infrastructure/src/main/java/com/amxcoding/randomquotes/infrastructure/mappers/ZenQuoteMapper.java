package com.amxcoding.randomquotes.infrastructure.mappers;

import com.amxcoding.randomquotes.domain.entities.Quote;
import com.amxcoding.randomquotes.infrastructure.models.ZenQuote;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ZenQuoteMapper {
    ZenQuote toZenQuote(Quote quote);
    Quote toQuote(ZenQuote zenQuote);


}
