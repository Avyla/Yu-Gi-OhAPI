package org.LeetCode.model;

import java.util.List;

public class Card {
    public int id;
    public String name;
    public String type;        // p.ej. "Effect Monster", "Link Monster", etc.
    public String desc;
    public Integer atk;        // puede venir null en algunos tipos
    public Integer def;        // en Link Monster normalmente viene null
    public Integer level;      // puede ser null en algunos tipos
    public Integer linkval;    // para Link Monsters
    public String race;        // p.ej. "Dragon"
    public String attribute;   // p.ej. "DARK"
    public List<CardImage> card_images;
    public List<CardPrice> card_prices;

    public boolean isMonster() {
        return type != null && type.contains("Monster");
    }
}
