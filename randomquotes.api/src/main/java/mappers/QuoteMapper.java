package mappers;

import entities.Quote;
import models.QuoteResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuoteMapper {
    // QuoteResponse
    QuoteResponse toQuoteResponse(Quote quote);
    List<QuoteResponse> toQuoteResponseList(List<Quote> quotes);


}
