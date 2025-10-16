/**
 * @author Rommel Isaac Baldivas
 */

package org.utdteamthreefive.backend.models;

import org.utdteamthreefive.backend.models.enums.WordType;
import org.utdteamthreefive.backend.models.interfaces.IWord;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Word implements IWord {
    private IntegerProperty wordID;
    private StringProperty wordToken;
    private IntegerProperty totalCount;
    private IntegerProperty startCount;
    private IntegerProperty endCount;
    private WordType type;

    public Word(int wordID, String wordToken, int totalCount, int startCount, int endCount, WordType type) {
        this.wordID = new SimpleIntegerProperty(this, "wordID", wordID);
        this.wordToken = new SimpleStringProperty(this, "wordToken", wordToken);
        this.totalCount = new SimpleIntegerProperty(this, "totalCount", totalCount);
        this.startCount = new SimpleIntegerProperty(this, "startCount", startCount);
        this.endCount = new SimpleIntegerProperty(this, "endCount", endCount);
        this.type = type;
    }

    public int getID() {
        return wordID.get();
    }

    public String getWordToken() {
        return wordToken.get();
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public int getStartCount() {
        return startCount.get();
    }

    public int getEndCount() {
        return endCount.get();
    }

    public WordType getType() {
        return this.type;
    }

    public int setTotalCount(int newCount) {
        this.totalCount.set(newCount);
        return this.totalCount.get();
    }

    public int setStartCount(int newCount) {
        this.startCount.set(newCount);
        return this.startCount.get();
    }

    public int setEndCount(int newCount) {
        this.endCount.set(newCount);
        return this.endCount.get();
    }
}
