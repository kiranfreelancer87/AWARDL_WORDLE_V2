package com.piddlepops.awardl;

public class PostResultModel {
    String name;
    long seconds;
    int numberOfGuesses;
    float score;
    String email;

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTime(long seconds) {
        this.seconds = seconds;
    }

    public void setNumberOfGuesses(int numberOfGuesses) {
        this.numberOfGuesses = numberOfGuesses;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public PostResultModel(String name, String email, long seconds, int numberOfGuesses, float score) {
        this.name = name;
        this.email = email;
        this.seconds = seconds;
        this.numberOfGuesses = numberOfGuesses;
        this.score = score;
    }
}
