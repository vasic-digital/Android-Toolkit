package com.redelf.widget.fastscrolleralphabet.models;

public class AlphabetItem {

    private int position;
    private String word;
    private boolean isActive;

    public AlphabetItem(int pos, String word, boolean isActive) {
        this.position = pos;
        this.word = word;
        this.isActive = isActive;
    }

    // Getters
    public int getPosition() {
        return position;
    }

    public String getWord() {
        return word;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setPosition(int position) {
        this.position = position;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
