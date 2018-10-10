package algorithms;

import db.DBconnection;
import literals.Literals;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Pgrnk_BM25.java - a java class that computes the combined page rank and okapi scores for the crawled documents
 * @author  Vishnu Narayanan Surendran
 * @version 1.0
 */

public class Pgrnk_BM25 {

    double alphaVal = 0.5;

    /**
     * This method computes the combined score and updates the feature table
     * @throws Exception
     */

    public void computePgrn_BM25() throws Exception
    {
        Connection conn = DBconnection.getCon(Literals.dbcon, true);
        Statement stmt = conn.createStatement();
        String cmbscore = "update features set pgrn_okapi_score = cmbscr.score from \n" +
                "(select documents.docid,term,(("+alphaVal+" * okapi_score) + ("+(1-alphaVal)+" * page_rank)) as score \n" +
                "from documents,features where features.docid=documents.docid) cmbscr \n" +
                "where cmbscr.docid=features.docid and cmbscr.term=features.term";
        stmt.executeUpdate(cmbscore);
        conn.close();
        stmt.close();
        System.out.println("Pgrn_BM25 computed");
    }

}
