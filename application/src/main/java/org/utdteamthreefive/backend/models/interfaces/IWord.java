/**
 * @author Rommel Isaac Baldivas
 */

package org.utdteamthreefive.backend.models.interfaces;

import org.utdteamthreefive.backend.models.enums.WordType;

public interface IWord {
    public int getID();

    public String getWordToken();

    public int getTotalCount();

    public int setTotalCount(int newCount);

    public int getStartCount();

    public int setStartCount(int newCount);

    public int getEndCount();

    public int setEndCount(int newCount);

    public WordType getType();
}
