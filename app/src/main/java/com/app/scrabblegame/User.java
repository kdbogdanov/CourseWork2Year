package com.app.scrabblegame;

public class User {
    private long id;
    private String name;
    private Integer score;
    private Integer passes;
    private Integer userMove;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getPasses() {
        return passes;
    }

    public void setPasses(Integer passes) {
        this.passes = passes;
    }

    public Integer getUserMove() {
        return userMove;
    }

    public void setUserMove(Integer userMove) {
        this.userMove = userMove;
    }
}
