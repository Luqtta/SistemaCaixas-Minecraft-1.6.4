package me.avelar.caixas;

import java.util.ArrayList;
import java.util.List;

public class Crate {
    public final String id;

    public boolean enabled = true;

    public String displayName;
    public List<String> desc = new ArrayList<String>();

    public String iconMaterial = "CHEST";

    public double price = 0.0;

    public final List<Reward> rewards = new ArrayList<Reward>();

    public Crate(String id) {
        this.id = id;
    }
}
