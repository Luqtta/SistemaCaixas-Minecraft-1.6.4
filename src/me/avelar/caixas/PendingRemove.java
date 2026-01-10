package me.avelar.caixas;

public class PendingRemove {
    public final String crateId;
    public final int index;

    public PendingRemove(String crateId, int index) {
        this.crateId = crateId;
        this.index = index;
    }
}
