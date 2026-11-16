package com.sports.management.ranking.model;

import java.util.List;

public class RankingResponse {
    private Ranking ranking;
    private List<RankingItem> items;

    public RankingResponse() {
    }

    public RankingResponse(Ranking ranking, List<RankingItem> items) {
        this.ranking = ranking;
        this.items = items;
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }

    public List<RankingItem> getItems() {
        return items;
    }

    public void setItems(List<RankingItem> items) {
        this.items = items;
    }
}
