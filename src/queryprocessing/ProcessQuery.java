package queryprocessing;

import db.DBconnection;
import indexer.HTMLStrip;
import literals.Literals;
import report.QueryExtension;
import servlet.FetchedResultData;
import servlet.ImagePOJO;
import servlet.Servletpojo;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ProcessQuery.java - a java class for processing the search query given by the user and return the results.
 *
 * @author Vishnu Narayanan Surendran
 * @version 1.0
 */
public class ProcessQuery extends Literals{

    /**
     * method searches the database for the documents matching the user search parameters
     *
     * @param searchQuery : gets the search string from the user
     * @param siteOp:     gets the value of the site operator if specified
     * @param kVal        : gets the limit for the result set
     * @param language    : gets the language value : English/German
     * @param model       : gets the scoring model value
     * @return
     */
    public Servletpojo fetchDocs(String searchQuery, String siteOp, int kVal, String language,int model) {
        ResultSet rs;
        String view=null;
        if(model==1)
            view="features_tfidf";
        else if(model==2)
            view="features_bm25";
        else
            view="features_cmbscr";
        Connection conn = null;
        try {
            conn = DBconnection.getCon(Literals.dbcon, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<FetchedResultData> listjson = new ArrayList<>();
        String selectString = "select a.docid as id, sum(score) as score , url from "+view+" a full outer join documents b  on b.docid = a.docid WHERE b.lang = '"+language+"' AND ";
        String searchConditon = "";
        String intersectString = "select docid as id from "+view+" WHERE ";
        String intersectionString = "";
        String queryToSearch;
        String siteOpSearch = "";
        int mode = 0;
        if (siteOp != null)
            siteOpSearch = "and url like '%" + siteOp.trim() + "%'";
        searchQuery = searchQuery.toLowerCase().replace("site:" + siteOp, "");
        String[] term = searchQuery.split(" ");
        List<String> strarray = new ArrayList<>();
        Servletpojo servletpojo = new Servletpojo();
        servletpojo.setSearchTerms(term);
        HTMLStrip stem = new HTMLStrip();
        String forStem;
        String wordToStem;
        for (String temp : term) {
            try {
                if (temp.startsWith("\"") && temp.endsWith("\"")) {
                    forStem = temp.replaceAll("\"", "");
                    if (forStem.startsWith("~")) {
                        wordToStem = forStem.replace("~", "");
                        forStem = stem.stemming(wordToStem);
                        strarray.add("\"" + "~" + forStem + "\"");
                    } else {
                        wordToStem = forStem;
                        forStem = stem.stemming(wordToStem);
                        strarray.add("\"" + forStem + "\"");
                    }
                } else if (temp.startsWith("~")) {
                    wordToStem = temp.replace("~", "");
                    forStem = stem.stemming(wordToStem);
                    strarray.add("~" + forStem);
                } else {
                    strarray.add(stem.stemming(temp));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String[] terms = strarray.toArray(new String[0]);

        servletpojo.setStemmedTerms(terms);
        //System.out.println("terms :" + Arrays.toString(terms));
        List<String> quotationTerms = new ArrayList<>();
        List<String> synonyms = new ArrayList<>();
        List<String> allTerms = new ArrayList<>();
        QueryExtension queryExtension = new QueryExtension();
        String temporary;
        System.out.println("searchQuery after replace :" + searchQuery);
        for (int i = 0; i < terms.length; i++) {
            //System.out.println("term :" + terms[i]);
            if (terms[i].length() > 1 && terms[i].startsWith("\"") && terms[i].endsWith("\"")) {
                temporary = terms[i].replaceAll("\"", "");
                if (temporary.startsWith("~")) {
                    mode = 1;
                    temporary = temporary.replace("~", "");
                    if (language == "en" || language.equalsIgnoreCase("en"))
                        synonyms = queryExtension.getsynengwordnet(temporary);
                    else
                        synonyms = queryExtension.getsyngermanwordnet(temporary);
                    quotationTerms.add(temporary);//terms[i].substring(terms[i].indexOf("\"") + 1, terms[i].lastIndexOf("\"")));
                    allTerms.add(temporary);
                } else {
                    mode=1;
                    quotationTerms.add(temporary);//terms[i].substring(terms[i].indexOf("\"") + 1, terms[i].lastIndexOf("\"")));
                    allTerms.add(temporary);//terms[i].substring(terms[i].indexOf("\"") + 1, terms[i].lastIndexOf("\"")));
                }
            } else if (terms[i].startsWith("~")) {
                mode = 1;
                temporary = terms[i].replace("~", "");
                if (language == "en" || language.equalsIgnoreCase("en"))
                    synonyms = queryExtension.getsynengwordnet(temporary);
                else
                    synonyms = queryExtension.getsyngermanwordnet(temporary);
                allTerms.add(temporary);
            } else
                allTerms.add(terms[i]);
        }
        String[] qtTerms = quotationTerms.toArray(new String[0]);
        String[] alTerms = allTerms.toArray(new String[0]);
        String[] synTerms = synonyms.toArray(new String[0]);
        System.out.println("quotationTerms:  " + qtTerms);
        System.out.println("allTerms:  " + alTerms);
        System.out.println("synonymTerms:  " + synTerms);
        if (qtTerms.length > 0)
            mode = 1;
        if (mode == 1) {
            for (int i = 0; i < qtTerms.length; i++) {
                //mode = 1;
                //System.out.println("quotationTerms:  " + qtTerms[i]);
                if (i == 0)
                    intersectionString = intersectString;
                if (i != (qtTerms.length - 1))
                    intersectionString = intersectionString.concat("LOWER(term) = '" + qtTerms[i].toLowerCase() + "' INTERSECT " + intersectString);
                else
                    intersectionString = intersectionString.concat("LOWER(term) = '" + qtTerms[i].toLowerCase() + "'");
            }
            if (synTerms.length > 0) {
                intersectionString = intersectionString.concat(intersectString + "LOWER(term) IN ( ");
                for (int i = 0; i < synTerms.length; i++) {
                    intersectionString = intersectionString.concat(" '" + synTerms[i] + "',");
                }
                intersectionString = intersectionString.substring(0, intersectionString.length() - 1);
                intersectionString = intersectionString.concat(" )");
            }
        }
        for (int i = 0; i < alTerms.length; i++) {
            //System.out.println("normalTerms:  " + alTerms[i]);
            if (i != (alTerms.length - 1))
                searchConditon = searchConditon.concat("LOWER(term) = '" + alTerms[i].toLowerCase() + "' OR ");
            else
                searchConditon = searchConditon.concat("LOWER(term) = '" + alTerms[i].toLowerCase() + "'");
        }
        if (synTerms.length > 0) {
            searchConditon = searchConditon.concat(" OR LOWER(term) In ( ");
            for (int i = 0; i < synTerms.length; i++) {
                searchConditon = searchConditon.concat(" '" + synTerms[i] + "',");
            }
            searchConditon = searchConditon.substring(0, searchConditon.length() - 1);
            searchConditon = searchConditon.concat(" )");
        }
        //System.out.println("searchConditon: where " + searchConditon);
        //System.out.println("intersectionString: where " + intersectionString);
        if (mode == 1)
            queryToSearch = selectString + "a.docid in ( " + intersectionString + " ) and (" + searchConditon + ")" + siteOpSearch +
                    " group by a.docid,url order by score desc LIMIT " + kVal;
        else
            queryToSearch = selectString + "(" + searchConditon + ")" + siteOpSearch + " group by a.docid,url order by score desc LIMIT " + kVal;
        System.out.println("queryToSearch:  " + queryToSearch);
        try {
            Statement stmt = conn.createStatement();
            rs = stmt.executeQuery(queryToSearch);
            int i = 1;
            System.out.println("Search Results:  ");
            while (rs.next()) {
                System.out.println("*******************************");
                System.out.println("Results :  ");
                System.out.println("doc id : " + rs.getInt("id"));
                System.out.println("url : " + rs.getString("url"));
                System.out.println("rank : " + i);
                System.out.println("score : " + rs.getDouble("score"));
                System.out.println("language : " + language);
                System.out.println("*******************************");
                FetchedResultData fetchedResultData = new FetchedResultData();
                fetchedResultData.setId(rs.getInt("id"));
                fetchedResultData.setScore(rs.getDouble("score"));
                fetchedResultData.setRank(i);
                fetchedResultData.setUrl(rs.getString("url"));
                listjson.add(fetchedResultData);
                i++;
            }
            conn.close();
            stmt.close();
            servletpojo.setK_val(kVal);
            servletpojo.setLanguage(language);
            servletpojo.setFetchedResultData(listjson);
            return servletpojo;
        } catch (SQLException e) {
            System.out.println("Catch block: getURLFromQueue");
            e.printStackTrace();
            System.out.println("getURLFromQueue return value:" + null);
            return servletpojo;
        }

    }

    public Servletpojo fetchImgDocs(String searchQuery, String siteOp, int kVal, String language) {
        ResultSet rs;
        Connection conn = null;
        try {
            conn = DBconnection.getCon(Literals.dbcon, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ImagePOJO> imglistjson = new ArrayList<>();
        String searchConditon = "";
        String intersectionString = "select imgid from img_features WHERE ";
        String queryToSearch;
        String siteOpSearch = "";
        String titleval="((case when (";
        String dscval="(case when (";
        String keywordval="(case when (";
        String altval="(case when (";
        double titlescore=  lambda * Math.pow(2.718281828459045,-(lambda * (window/5)));
        double dscscore=  lambda * Math.pow(2.718281828459045,-(lambda * (window+1)));
        double keywordscore=  lambda * Math.pow(2.718281828459045,-(lambda * (window+1)));
        double altscore=  lambda * Math.pow(2.718281828459045,-(lambda * 1));
        int mode = 0;
        if (siteOp != null)
            siteOpSearch = "and imgurl like '%" + siteOp.trim() + "%'";
        searchQuery = searchQuery.toLowerCase().replace("site:" + siteOp, "");
        String[] term = searchQuery.split(" ");
        List<String> strarray = new ArrayList<>();
        Servletpojo servletpojo = new Servletpojo();
        servletpojo.setSearchTerms(term);
        HTMLStrip stem = new HTMLStrip();
        String forStem;
        for (String temp : term) {
            try {
                if (temp.startsWith("\"") && temp.endsWith("\"")) {
                    forStem =  temp.replaceAll("\"", "");
                    forStem = stem.stemming(forStem);
                    strarray.add("\"" +forStem+"\"" );
                } else {
                    strarray.add(stem.stemming(temp));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String[] terms = strarray.toArray(new String[0]);

        servletpojo.setStemmedTerms(terms);
        //System.out.println("terms :" + Arrays.toString(terms));
        List<String> quotationTerms = new ArrayList<String>();
        List<String> allTerms = new ArrayList<String>();
        System.out.println("searchQuery after replace :" + searchQuery);
        for (int i = 0; i < terms.length; i++) {
            //System.out.println("term :" + terms[i]);
            if (terms[i].length() > 1 && terms[i].startsWith("\"") && terms[i].endsWith("\"")) {
                quotationTerms.add(terms[i].substring(terms[i].indexOf("\"") + 1, terms[i].lastIndexOf("\"")));
                allTerms.add(terms[i].substring(terms[i].indexOf("\"") + 1, terms[i].lastIndexOf("\"")));
            } else
                allTerms.add(terms[i]);
        }
        String[] qtTerms = quotationTerms.toArray(new String[0]);
        String[] alTerms = allTerms.toArray(new String[0]);
        for (int i = 0; i < qtTerms.length; i++) {
            mode = 1;
            //System.out.println("quotationTerms:  " + qtTerms[i]);
            if (i != (qtTerms.length - 1))
                intersectionString = intersectionString.concat("LOWER(terms) = '" + qtTerms[i].toLowerCase() + "' INTERSECT " + intersectionString);
            else
                intersectionString = intersectionString.concat("LOWER(terms) = '" + qtTerms[i].toLowerCase() + "'");
        }
        for (int i = 0; i < alTerms.length; i++) {
            //System.out.println("normalTerms:  " + alTerms[i]);
            if (i != (alTerms.length - 1))
            {
                searchConditon = searchConditon.concat("LOWER(terms) = '" + alTerms[i].toLowerCase() + "' OR ");
                titleval= titleval.concat("LOWER(title) like '%" + alTerms[i].toLowerCase() + "%' OR ");
                dscval=dscval.concat("LOWER(description) like '%" + alTerms[i].toLowerCase() + "%' OR ");
                keywordval=keywordval.concat("LOWER(keyword) like '%" + alTerms[i].toLowerCase() + "%' OR ");
                altval=altval.concat("LOWER(alt_tag) like '%" + alTerms[i].toLowerCase() + "%' OR ");
            }
            else
            {
                searchConditon = searchConditon.concat("LOWER(terms) = '" + alTerms[i].toLowerCase() + "'");
                titleval= titleval.concat("LOWER(title) like '%" + alTerms[i].toLowerCase() + "%') then " + titlescore + " else "+ 0 + " end )");
                dscval=dscval.concat("LOWER(description) like '%" + alTerms[i].toLowerCase() + "%') then " + dscscore + " else "+ 0 + " end )");
                keywordval=keywordval.concat("LOWER(keyword) like '%" + alTerms[i].toLowerCase() + "%') then " + keywordscore + " else "+ 0 + " end )");
                altval=altval.concat("LOWER(alt_tag) like '%" + alTerms[i].toLowerCase() + "%') then " + altscore + " else "+ 0 + " end ))");
            }
        }
        String selectString="select imgf.imgid, (sum(imgf.imgscore)  + "+titleval+" + "+dscval+" + "+keywordval+" + "+altval+") as score," +
                "imgurl from (img_features imgf full outer join images img on img.imgid=imgf.imgid) full outer join metadata md " +
                "on img.docid=md.docid WHERE lang = '"+language+"' and ";
        //System.out.println("searchConditon: where " + searchConditon);
        //System.out.println("intersectionString: where " + intersectionString);
        if (mode == 1)
            queryToSearch = selectString + "imgid in ( " + intersectionString + " ) and (" + searchConditon + ")" + siteOpSearch +
                    " group by imgf.imgid,imgurl,title,description,keyword,alt_tag order by score desc LIMIT " + kVal;
        else
            queryToSearch = selectString + "(" + searchConditon + ")" + siteOpSearch + " group by imgf.imgid,imgurl,title,description,keyword,alt_tag order by score desc LIMIT " + kVal;
        System.out.println("queryToSearch:  " + queryToSearch);
        try {
            int imgId;
            int imgRank;
            String imgUrl;
            Double imgScore;
            Statement stmt = conn.createStatement();
            rs = stmt.executeQuery(queryToSearch);
            int i = 1;
            System.out.println("Search Results:  ");
            while (rs.next()) {
                System.out.println("*******************************");
                System.out.println("Results :  ");
                System.out.println("img id : " + rs.getInt("imgid"));
                System.out.println("url : " + rs.getString("imgurl"));
                System.out.println("rank : " + i);
                System.out.println("score : " + rs.getDouble("score"));
                System.out.println("language : " + language);
                System.out.println("*******************************");
                ImagePOJO fetchedResultData = new ImagePOJO();
                imgId = rs.getInt("imgid");
                imgScore = rs.getDouble("score");
                imgUrl = rs.getString("imgurl");
                fetchedResultData.setImgId(rs.getInt("imgid"));
                fetchedResultData.setImgScore(rs.getDouble("score"));
                fetchedResultData.setImgRank(i);
                fetchedResultData.setImgUrl(rs.getString("imgurl"));
                imglistjson.add(fetchedResultData);
                i++;
            }
            conn.close();
            stmt.close();
            servletpojo.setK_val(kVal);
            servletpojo.setLanguage(language);
            servletpojo.setImageResultData(imglistjson);
            return servletpojo;
        } catch (SQLException e) {
            System.out.println("Catch block: getURLFromQueue");
            e.printStackTrace();
            System.out.println("getURLFromQueue return value:" + null);
            return servletpojo;
        }

    }
}
