package vn.kir.keiba;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;import java.nio.file.Path;import java.sql.*;import java.util.UUID;import static org.junit.jupiter.api.Assertions.*;
class RaceRepositoryMigrationTest {
    @TempDir Path folder;
    @Test void migratesMvpOneAndAcceptsNewTicket()throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+folder.resolve("keiba.db"));Statement s=c.createStatement()){s.executeUpdate("CREATE TABLE races(id INTEGER PRIMARY KEY AUTOINCREMENT,state TEXT NOT NULL,seed INTEGER NOT NULL,seed_hash TEXT NOT NULL,opened_at INTEGER NOT NULL,close_at INTEGER NOT NULL,horse_first INTEGER,horse_second INTEGER,total_pool REAL NOT NULL DEFAULT 0,paid_pool REAL NOT NULL DEFAULT 0,settled_at INTEGER)");s.executeUpdate("CREATE TABLE tickets(id INTEGER PRIMARY KEY AUTOINCREMENT,race_id INTEGER NOT NULL REFERENCES races(id),player_uuid TEXT NOT NULL,player_name TEXT NOT NULL,horse_a INTEGER NOT NULL,horse_b INTEGER NOT NULL,stake REAL NOT NULL,odds_at_bet REAL NOT NULL,odds_at_close REAL,payout REAL NOT NULL DEFAULT 0,settled INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL)");s.executeUpdate("CREATE TABLE audit_log(id INTEGER PRIMARY KEY AUTOINCREMENT,created_at INTEGER NOT NULL,action TEXT NOT NULL,race_id INTEGER,actor TEXT NOT NULL,detail TEXT NOT NULL)");}
        try(RaceRepository repo=new RaceRepository(folder)){long race=repo.createRace(1,"hash",System.currentTimeMillis()+1000);long id=repo.insertTicket(race,UUID.randomUUID(),"Tester",BetType.TRIFECTA,"2-5-1",20000,12);BetTicket ticket=repo.tickets(race).get(0);assertTrue(id>0);assertEquals(BetType.TRIFECTA,ticket.type());assertEquals("2-5-1",ticket.selection());}
    }
    @Test void resetAllRestartsRaceAndTicketIdsAtOne()throws Exception{
        try(RaceRepository repo=new RaceRepository(folder)){
            long oldRace=repo.createRace(1,"old",System.currentTimeMillis()+1000);
            long oldTicket=repo.insertTicket(oldRace,UUID.randomUUID(),"Tester",BetType.WIN,"1",10000,1.2);
            assertEquals(1,oldRace);assertEquals(1,oldTicket);
            repo.resetAll();
            long newRace=repo.createRace(2,"new",System.currentTimeMillis()+1000);
            long newTicket=repo.insertTicket(newRace,UUID.randomUUID(),"Tester",BetType.WIN,"1",10000,1.2);
            assertEquals(1,newRace);assertEquals(1,newTicket);
        }
    }
}
