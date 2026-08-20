package vn.kir.keiba;
import org.junit.jupiter.api.Test;import java.util.List;import static org.junit.jupiter.api.Assertions.*;
class BetTypeTest {
    private final List<HorseEntry> order=List.of(horse(2),horse(5),horse(1),horse(3));
    @Test void winAndPlace(){assertTrue(BetType.WIN.wins("2",order));assertFalse(BetType.WIN.wins("5",order));assertTrue(BetType.PLACE.wins("1",order));assertFalse(BetType.PLACE.wins("3",order));}
    @Test void umarenIgnoresOrder(){assertEquals("2-5",BetType.UMAREN.key(List.of(5,2)));assertTrue(BetType.UMAREN.wins("2-5",order));}
    @Test void exactaAndTrifectaRequireOrder(){assertTrue(BetType.EXACTA.wins("2-5",order));assertFalse(BetType.EXACTA.wins("5-2",order));assertTrue(BetType.TRIFECTA.wins("2-5-1",order));assertFalse(BetType.TRIFECTA.wins("2-1-5",order));}
    private static HorseEntry horse(int number){return new HorseEntry(number,"H"+number,RunningStyle.SENKO,70,70,70);}
}
