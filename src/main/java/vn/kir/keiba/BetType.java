package vn.kir.keiba;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

enum BetType {
    WIN("Thắng", 1, true), PLACE("Vào top 3", 1, true), UMAREN("Umaren", 2, false), EXACTA("Exacta", 2, true), TRIFECTA("Trifecta", 3, true);
    final String label; final int selections; final boolean ordered;
    BetType(String label,int selections,boolean ordered){this.label=label;this.selections=selections;this.ordered=ordered;}
    String key(List<Integer> horses){List<Integer> values=ordered?List.copyOf(horses):horses.stream().sorted().toList();return values.stream().map(String::valueOf).collect(Collectors.joining("-"));}
    String winningKey(List<HorseEntry> order){return switch(this){case WIN->""+order.get(0).number();case PLACE->"*";case UMAREN->key(List.of(order.get(0).number(),order.get(1).number()));case EXACTA->key(List.of(order.get(0).number(),order.get(1).number()));case TRIFECTA->key(List.of(order.get(0).number(),order.get(1).number(),order.get(2).number()));};}
    boolean wins(String selection,List<HorseEntry> order){if(this==PLACE){int horse=Integer.parseInt(selection);return order.stream().limit(3).anyMatch(h->h.number()==horse);}return selection.equals(winningKey(order));}
    static List<Integer> parse(String key){return Arrays.stream(key.split("-")).map(Integer::parseInt).toList();}
}
