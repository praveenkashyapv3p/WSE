package db;

import literals.Literals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * DBTables.java - a java class that contains methods for handling the various DB operations
 * @author  Vishnu Narayanan Surendran and Praveen Kashyap
 * @version 1.0
 */



public class DBTables extends Literals {
    /**
     * Creates tables when crawler starts
     * @throws Exception
     */
    public void createTables() throws Exception {
        Statement stmt;
        String links_table = "create table if not exists links(from_docid integer, to_docid integer)";
        String meta = "create table if not exists metadata(id serial not null  PRIMARY KEY,docid integer not null, author text, description text, keyword text, title text)";
        String documents = "create table if not exists documents(docid integer PRIMARY KEY , url text, crawled_on_date text, lang text, page_rank double PRECISION,content text)";
        String content = "CREATE TABLE if not exists content(docid INTEGER,title TEXT, fullcontent TEXT);";
        String snippet = "create table IF NOT EXISTS snippet(docid integer, term text, snippet text);";
        String images = "create table if not exists images(imgid serial not null PRIMARY KEY ,docid integer, imgurl text, alt_tag text,lang text)";
        String crawlqueue = "create table if not exists crawlqueue(id serial not null PRIMARY KEY ,url text,crawl_status text,level integer,parent_doc integer)";
        String features = "create table if not exists features(docid integer not null ,term text not null,term_frequency integer not null,tf double precision,idf double precision,score double precision,okapi_score double precision,pgrn_okapi_score double precision, PRIMARY KEY(docid,term))";
        String img_features = "create table if not exists img_features(id serial not null PRIMARY KEY,imgid integer,terms text,imgscore double precision)";
        String dictTable = "create table if not exists t_dictionary(id SERIAL not null, plainword text,stemmedword text)";
        String shingles = "create table if not exists shingles(id INTEGER, shingle text,md5hashval INTEGER);";
        shingles+= "create or replace function h_int(text) returns int as $$\n" +  "select ('x'||substr(md5($1),1,8))::bit(32)::int;\n" + "$$ language sql;";
        String jaccardSimilarity = "CREATE TABLE IF NOT EXISTS jaccardsim(docid1 INTEGER,docid2 INTEGER,jacardsim DOUBLE PRECISION,jaccard1 DOUBLE PRECISION DEFAULT 0,jaccard4 DOUBLE PRECISION DEFAULT 0,jaccard16 DOUBLE PRECISION DEFAULT 0,jaccard32 DOUBLE PRECISION DEFAULT 0);";
        String advertisement = "create table IF NOT EXISTS advertisement(id serial not null, advertitle text, adverturl text, textofad text, tags text, budget double precision, advertimage text, cust_name text, cust_mailid text);" ;
        String metaSearch = "CREATE TABLE IF NOT EXISTS metaurls (id SERIAL PRIMARY KEY, metaurl TEXT,state INTEGER,cwval bigint);";
        String metaSearchTerms = "CREATE TABLE IF NOT EXISTS metasearchterms (id SERIAL PRIMARY KEY,collectionid INTEGER,term text,dfval bigint,rmin double precision,rmax double precision,coriscore double precision);";
        String metaSearchDocs = "CREATE TABLE IF NOT EXISTS metasearchdocs (id SERIAL PRIMARY KEY, collectionid INTEGER,docurl TEXT,score double precision);";
        Connection conn = DBconnection.getCon(dbcon, true);
        stmt = conn.createStatement();
        stmt.executeUpdate(links_table);
        stmt.executeUpdate(meta);
        stmt.executeUpdate(documents);
        stmt.executeUpdate(content);
        stmt.executeUpdate(snippet);
        stmt.executeUpdate(images);
        stmt.executeUpdate(crawlqueue);
        stmt.executeUpdate(features);
        stmt.executeUpdate(img_features);
        stmt.executeUpdate(dictTable);
        stmt.executeUpdate(advertisement);
        stmt.executeUpdate(shingles);
        stmt.executeUpdate(jaccardSimilarity);
        stmt.executeUpdate(metaSearch);
        stmt.executeUpdate(metaSearchDocs);
        stmt.executeUpdate(metaSearchTerms);
        stmt.close();
        conn.close();
    }


    /**
     * Initialize crawler queue with urls to crawl
     * @param arr : list of urls
     * @throws Exception
     */
    public void initializeQueue(String[] arr) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        String ins_query = "INSERT INTO crawlqueue(url,crawl_status,level,parent_doc) VALUES (?,?,?,?);";
        PreparedStatement pstmt = conn.prepareStatement(ins_query);
        for (int i = 0; i < arr.length; i++) {
            pstmt.setString(1, arr[i]);
            pstmt.setString(2, "initial");
            pstmt.setInt(3, 1);
            pstmt.setInt(4, 0);
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        System.out.println("");
        conn.close();
        pstmt.close();
    }
    /**
     * This method creates Views on the feature table for the various scoring models
     * @throws Exception
     */
    public void createViews() throws Exception{
        Statement stmt;
        String features_tfidf="create or replace view features_tfidf as \n" +
                "select \n" +
                "    docid,\n" +
                "    term,\n" +
                "    score\n" +
                "from features;";
        String features_bm25="create or replace view features_bm25 as \n" +
                "select \n" +
                "    docid,\n" +
                "    term,\n" +
                "    okapi_score as score\n" +
                "from features;";
        String features_cmbscr="create or replace view features_cmbscr as \n" +
                "select \n" +
                "    docid,\n" +
                "    term,\n" +
                "    pgrn_okapi_score as score\n" +
                "from features;";
        Connection conn = DBconnection.getCon(dbcon,true);
        stmt = conn.createStatement();
        stmt.executeUpdate(features_tfidf);
        stmt.executeUpdate(features_bm25);
        stmt.executeUpdate(features_cmbscr);
        stmt.close();
        conn.close();

    }

    /**
     * Drop Indexes
     * @throws Exception
     */
    public void dropIndexes() throws Exception {
        Statement stmt;
        String crawlqueue = "DROP INDEX IF EXISTS queue_index";
        String documents = "DROP INDEX IF EXISTS doc_index";
        String features = "DROP INDEX IF EXISTS features_index";
        Connection conn = DBconnection.getCon(dbcon, true);
        stmt = conn.createStatement();
        stmt.executeUpdate(crawlqueue);
        stmt.executeUpdate(documents);
        stmt.executeUpdate(features);
        stmt.close();
        conn.close();
    }

    /**
     * Creates indexes of features table for column term, crawlqueue table for url and documents table for docid
     * @throws Exception
     */
    public void createIndexes() throws Exception {
        Statement stmt;
        String crawlqueue = "CREATE INDEX IF NOT EXISTS queue_index ON crawlqueue USING GIN(to_tsvector('english', url))";
        String documents = "CREATE INDEX IF NOT EXISTS doc_index ON documents(docid)";
        // String features = "CREATE INDEX IF NOT EXISTS features_index ON features USING GIN(docid,to_tsvector('english',term))";
        String features_term = "CREATE INDEX IF NOT EXISTS features_index ON features USING GIN (to_tsvector('english',term))";
        Connection conn = DBconnection.getCon(dbcon, true);
        stmt = conn.createStatement();
        stmt.executeUpdate(crawlqueue);
        stmt.executeUpdate(documents);
        ///stmt.executeUpdate(features);
        stmt.executeUpdate(features_term);
        stmt.close();
        conn.close();

    }
}
