package com.sports.management.result.model;

import java.util.List;

public class ResultResponse {
    private Result result;
    private List<ResultDetail> details;

    public ResultResponse() {
    }

    public ResultResponse(Result result, List<ResultDetail> details) {
        this.result = result;
        this.details = details;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public List<ResultDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ResultDetail> details) {
        this.details = details;
    }
}
