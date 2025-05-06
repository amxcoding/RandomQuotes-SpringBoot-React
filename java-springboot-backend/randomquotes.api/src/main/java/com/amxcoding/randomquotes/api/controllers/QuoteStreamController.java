package com.amxcoding.randomquotes.api.controllers;

import com.amxcoding.randomquotes.api.interfaces.IQuoteBroadCaster;
import com.amxcoding.randomquotes.api.models.quote.QuoteResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("sse/v1/quotes")
public class QuoteStreamController {

    private final IQuoteBroadCaster quoteBroadCaster;

    public QuoteStreamController(IQuoteBroadCaster quoteBroadCaster) {
        this.quoteBroadCaster = quoteBroadCaster;
    }

    @GetMapping(value = "/likes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<QuoteResponse> streamLikedQuotes() {
        return quoteBroadCaster.getFlux();
    }
}
