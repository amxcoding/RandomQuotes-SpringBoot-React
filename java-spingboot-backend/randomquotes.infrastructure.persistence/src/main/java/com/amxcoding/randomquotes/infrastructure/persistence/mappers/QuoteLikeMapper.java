package com.amxcoding.randomquotes.infrastructure.persistence.mappers;

import com.amxcoding.randomquotes.domain.entities.QuoteLike;
import com.amxcoding.randomquotes.infrastructure.persistence.models.QuoteLikeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuoteLikeMapper {

    @Mapping(target = "id", source = "id")
    QuoteLikeEntity toQuoteLikeEntity(QuoteLike quoteLike);

    @Mapping(target = "id", source = "id")
    QuoteLike toQuoteLike(QuoteLikeEntity quoteLike);


}
