package algorithms;

import db.DBconnection;
import literals.Literals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Okapi_BM25.java - a java class that computes the okapi scores for the crawled documents
 * @author  Vishnu Narayanan Surendran
 * @version 1.0
 */

public class Okapi_BM25 {
    double k=2.0;
    double b=0.75;

    /**
     * This method computes the average document length in the entire collection of documents
     * @param conn : contains the connection object
     * @param nVal : contains the document count in the document collection
     * @return : returns the average document length
     * @throws Exception
     */

    private int computeAvgdl(Connection conn,int nVal) throws Exception
    {
        int avgdl=0;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT (sum(term_frequency)/"+nVal+") as avgdl from features");
        while (rs.next()) {
            avgdl= rs.getInt("avgdl");

        }
        return avgdl;
    }

    /**
     * This method computes the total document count in the document collection
     * @param conn : contains the connection object
     * @return : returns the document count
     * @throws Exception
     */

    private int docCount(Connection conn) throws Exception
    {
        int docCnt=0;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(docid) as count FROM documents");
        while (rs.next()) {
            docCnt= rs.getInt("count");

        }
        return docCnt;
    }

    /**
     * This method computes the okapi score and updates the features table.
     * @throws Exception
     */

    public void computeOkapi_BM25() throws Exception
    {
        Connection conn = DBconnection.getCon(Literals.dbcon, true);
        Statement stmt = conn.createStatement();
        int nVal = docCount(conn);
        int avgdl = computeAvgdl(conn,nVal);
        String cal_okapi= "update features set okapi_score = okapiscore.score from \n" +
                "  (select a.docid,a.term,(WITH tb as (select count(docid) as nq from features where term=a.term) \n" +
                "  select (log (("+nVal+"-(select nq from tb) +0.5) / ((select nq from tb) + 0.5)) * \n" +
                "  ((term_frequency * (2.0  +1))/((term_frequency + 2.0 * (1-0.75 + 0.75 * ((select sum(term_frequency) from features where docid =a.docid)/"+avgdl+"))))))) \n" +
                "  as score from features a ) okapiscore where okapiscore.docid=features.docid \n" +
                "  and okapiscore.term=features.term ";
        stmt.executeUpdate(cal_okapi);
        conn.close();
        stmt.close();
        System.out.println("Okapi_BM25 computed");
    }

}
