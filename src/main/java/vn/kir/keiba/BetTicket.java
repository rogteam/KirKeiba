package vn.kir.keiba;
import java.util.UUID;
record BetTicket(long id,long raceId,UUID playerId,String playerName,BetType type,String selection,double stake,double oddsAtBet,boolean settled,double payout){ }
