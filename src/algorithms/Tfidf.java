package algorithms;

import db.DBconnection;
import literals.Literals;

import java.sql.Connection;
import java.sql.Statement;

public class Tfidf extends Literals {
    /**
     * calculate tf, idf and score and update into DB (table name : features)
     * @throws Exception
     */
    public void tfidf() throws Exception {
        Statement stmt;
        Connection c = DBconnection.getCon(dbcon, true);
        stmt = c.createStatement();
        stmt.executeUpdate("UPDATE features SET tf=(1+log(term_frequency)),idf=abs(idf_alias.idf_calc) FROM (SELECT term,log(((SELECT  count(DISTINCT(docid)) AS N FROM documents) ::FLOAT)/((count(DISTINCT(docid))::FLOAT))) AS idf_calc FROM features GROUP BY term) idf_alias WHERE idf_alias.term=features.term;");
        stmt.executeUpdate("UPDATE features SET score=tf*idf");
        c.close();
        stmt.close();
    }
}
