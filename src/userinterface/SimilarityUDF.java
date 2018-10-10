package userinterface;

import db.DBconnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static literals.Literals.dbcon;

public class SimilarityUDF {
    public void similarity(int docid, float threshold) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        String sql1 = "select docid2 from jaccardsim where docid1 = " + docid + " and jacardsim>=" + threshold + "";
        String sql2 = "select docid1 from jaccardsim where docid2 = " + docid + " and jacardsim>=" + threshold + "";
        ResultSet rs = stmt.executeQuery(sql1);
        int document;
        List<Integer> documents = new ArrayList<>();
        while (rs.next()) {
            document = rs.getInt(1);
            documents.add(document);
        }
        ResultSet rs2 = stmt.executeQuery(sql2);
        while (rs2.next()) {
            documents.add(rs2.getInt(1));
        }
        List<Integer> deduped = documents.stream().distinct().collect(Collectors.toList());
        System.out.println("Documents which are similar to " + docid + " and above threshold " + threshold + " are:\n" );
        for (int docsid : deduped){
            System.out.println("Similar Document id: "+docsid);
        }
        stmt.close();
        conn.close();
    }

    public void minHashing(int n) throws Exception {
        createSignature(n);
        insertintosignature(n);
    }

    public void createSignature(int n) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        String signatureTable = "DROP TABLE IF EXISTS signature;";
         signatureTable+= "CREATE TABLE IF NOT EXISTS signature AS (SELECT id,md5hashval FROM (\n" +
                " SELECT id,md5hashval,row_number() OVER (PARTITION BY id ORDER BY md5hashval ASC) minvalues FROM shingles) d\n" +
                "WHERE minvalues <= "+n+" ORDER BY id ASC);";
        stmt.executeUpdate(signatureTable);
        stmt.close();
        conn.close();
    }

    public void insertintosignature(int n) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        switch (n) {
            case 1:
                stmt.executeUpdate("UPDATE jaccardsim j SET jaccard1= newtab.minhashscore FROM(SELECT DISTINCT(a.id) AS docid1,b.id AS docid2,\n" +
                        "COUNT(DISTINCT(a.md5hashval))::FLOAT/(SELECT count(*) from(\n" +
                        "(SELECT DISTINCT id,s1.md5hashval from signature s1 WHERE s1.id=a.id GROUP BY s1.id,s1.md5hashval)  UNION\n" +
                        "((SELECT DISTINCT id,s2.md5hashval FROM signature  s2 WHERE s2.id=b.id GROUP BY s2.id,s2.md5hashval)))AS s3) AS minhashscore\n" +
                        "FROM signature b, signature a WHERE a.id <> b.id AND a.id<b.id AND a.md5hashval=b.md5hashval GROUP BY docid1,docid2\n" +
                        "ORDER BY docid1,docid2) newtab WHERE j.docid1=newtab.docid1 AND j.docid2=newtab.docid2;");
                break;
            case 4:
                stmt.executeUpdate("UPDATE jaccardsim j SET jaccard4= newtab.minhashscore FROM(SELECT DISTINCT(a.id) AS docid1,b.id AS docid2,\n" +
                        "COUNT(DISTINCT(a.md5hashval))::FLOAT/(SELECT count(*) from(\n" +
                        "(SELECT DISTINCT id,s1.md5hashval from signature s1 WHERE s1.id=a.id GROUP BY s1.id,s1.md5hashval)  UNION\n" +
                        "((SELECT DISTINCT id,s2.md5hashval FROM signature  s2 WHERE s2.id=b.id GROUP BY s2.id,s2.md5hashval)))AS s3) AS minhashscore\n" +
                        "FROM signature b, signature a WHERE a.id <> b.id AND a.id<b.id AND a.md5hashval=b.md5hashval GROUP BY docid1,docid2\n" +
                        "ORDER BY docid1,docid2) newtab WHERE j.docid1=newtab.docid1 AND j.docid2=newtab.docid2;");
                break;
            case 16:
                stmt.executeUpdate("UPDATE jaccardsim j SET jaccard16= newtab.minhashscore FROM(SELECT DISTINCT(a.id) AS docid1,b.id AS docid2,\n" +
                        "COUNT(DISTINCT(a.md5hashval))::FLOAT/(SELECT count(*) from(\n" +
                        "(SELECT DISTINCT id,s1.md5hashval from signature s1 WHERE s1.id=a.id GROUP BY s1.id,s1.md5hashval)  UNION\n" +
                        "((SELECT DISTINCT id,s2.md5hashval FROM signature  s2 WHERE s2.id=b.id GROUP BY s2.id,s2.md5hashval)))AS s3) AS minhashscore\n" +
                        "FROM signature b, signature a WHERE a.id <> b.id AND a.id<b.id AND a.md5hashval=b.md5hashval GROUP BY docid1,docid2\n" +
                        "ORDER BY docid1,docid2) newtab WHERE j.docid1=newtab.docid1 AND j.docid2=newtab.docid2;");
                break;
            case 32:
                stmt.executeUpdate("UPDATE jaccardsim j SET jaccard32= newtab.minhashscore FROM(SELECT DISTINCT(a.id) AS docid1,b.id AS docid2,\n" +
                        "COUNT(DISTINCT(a.md5hashval))::FLOAT/(SELECT count(*) from(\n" +
                        "(SELECT DISTINCT id,s1.md5hashval from signature s1 WHERE s1.id=a.id GROUP BY s1.id,s1.md5hashval)  UNION\n" +
                        "((SELECT DISTINCT id,s2.md5hashval FROM signature  s2 WHERE s2.id=b.id GROUP BY s2.id,s2.md5hashval)))AS s3) AS minhashscore\n" +
                        "FROM signature b, signature a WHERE a.id <> b.id AND a.id<b.id AND a.md5hashval=b.md5hashval GROUP BY docid1,docid2\n" +
                        "ORDER BY docid1,docid2) newtab WHERE j.docid1=newtab.docid1 AND j.docid2=newtab.docid2;");
                break;
            default:
                System.out.println("invalid input!!! Select values only rom(1,4,16,32)");
        }
        stmt.close();
        conn.close();
    }


    public static void main(String[] args) throws Exception {
        SimilarityUDF similarityUDF = new SimilarityUDF();
        System.out.println("Enter document id to search for similar documents:");
        Scanner reader = new Scanner(System.in);
        int docid = reader.nextInt();
        System.out.println("Enter threshold:");
        float threshold = reader.nextFloat();
        similarityUDF.similarity(docid, threshold);
        similarityUDF.minHashing(1);
        similarityUDF.minHashing(4);
        similarityUDF.minHashing(16);
        similarityUDF.minHashing(32);
    }
}
