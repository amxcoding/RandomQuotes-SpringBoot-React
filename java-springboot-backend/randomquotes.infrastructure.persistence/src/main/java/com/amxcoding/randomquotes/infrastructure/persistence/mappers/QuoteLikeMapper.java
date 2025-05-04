package com.amxcoding.randomquotes.infrastructure.persistence.mappers;

import com.amxcoding.randomquotes.domain.entities.QuoteLike;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteLikeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuoteLikeMapper {
    QuoteLikeEntity toQuoteLikeEntity(QuoteLike quoteLike);
    QuoteLike toQuoteLike(QuoteLikeEntity quoteLike);


}
