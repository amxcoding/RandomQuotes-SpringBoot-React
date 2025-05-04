package com.amxcoding.randomquotes.application.models;


import com.amxcoding.randomquotes.domain.entities.Quote;

import java.util.List;

public class QuoteListWrapper {
    private List<Quote> quotes;

    public QuoteListWrapper() {}

    public QuoteListWrapper(List<Quote> quotes) { this.quotes = quotes; }

    public List<Quote> getQuotes() { return quotes; }
    public void setQuotes(List<Quote> quotes) { this.quotes = quotes; }
}