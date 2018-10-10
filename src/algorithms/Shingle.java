package algorithms;

import db.DBconnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import static literals.Literals.dbcon;

public class Shingle {
    public void shingling(int docid,List<String> rawText)throws Exception{
        int k=4;
        String shingleString;
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        List<String> list_shingle;
        for (int i = 0; i<rawText.size()-k;i++){
            list_shingle = rawText.subList(i,i+k);
            shingleString = list_shingle.stream().collect(Collectors.joining(" "));
            stmt.executeUpdate("INSERT INTO shingles(id,shingle) VALUES("+docid+",'"+shingleString+"')");
        }

        stmt.close();
        conn.close();

    }

    public void createShingleSimilarity()throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String jacard = "SELECT DISTINCT a.id AS doc1,b.id AS doc2,\n" +
                "  count(DISTINCT(a.shingle))::FLOAT/(SELECT count(*) FROM((SELECT DISTINCT s1.shingle FROM shingles s1 WHERE s1.id=a.id GROUP BY s1.shingle)  UNION\n" +
                "                                    ((SELECT DISTINCT s2.shingle FROM shingles  s2 WHERE s2.id=b.id GROUP BY s2.shingle)))AS s3)\n" +
                "FROM shingles b, shingles a WHERE a.id <> b.id AND a.id<b.id AND a.shingle=b.shingle GROUP BY doc1,doc2\n" +
                "ORDER BY doc1,doc2;";
        ResultSet rs = stmt.executeQuery(jacard);
        while (rs.next()) {
            insertintojaccardsim(rs.getInt(1),rs.getInt(2),rs.getFloat(3));
        }
        stmt.close();
        conn.close();
    }

    public void insertintojaccardsim(int doc1, int doc2, float jaccard)throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("INSERT INTO jaccardSim VALUES (" + doc1 + "," + doc2 + "," + jaccard + ")");
        stmt.close();
        conn.close();
    }

    public void updateShingle()throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        System.out.println("updating shingle");
        stmt.executeUpdate("UPDATE shingles SET md5hashval=h_int(shingle)");
        stmt.close();
        conn.close();
    }



}


