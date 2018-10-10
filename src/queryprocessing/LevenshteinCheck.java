package queryprocessing;

import db.DBconnection;
import literals.Literals;
import userinterface.SearchReq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevenshteinCheck {
    List<String> containingTerms = new ArrayList<>();
    /**
     * Checks whether the input request can be processed from DB or not
     *
     * @param queryRequest : user input
     * @throws Exception
     */
    public String checkForTypo(String queryRequest) throws Exception {
        Connection conn = DBconnection.getCon(Literals.dbcon, true);
        Statement stmt = conn.createStatement();
        int index;
        String removeSite;
        String corrected;
        String addQuotes = "";
        String addTilde = "";
        String perfectTermString;
        String termsString;
        String siteValue = null;
        String[] termsArray;
        String[] siteRemoved;
        boolean siteFlag = false;
        boolean quoteFlag = false;
        boolean tildeFlag = false;
        SearchReq searchReq = new SearchReq();
        if (queryRequest.toLowerCase().contains("site:".toLowerCase())) {
            siteFlag = true;
            siteValue = searchReq.processSearchString(queryRequest);
            removeSite = queryRequest.replace("site:", "").replace(siteValue, "");
            siteRemoved = removeSite.split(" ");
        } else {
            siteRemoved = queryRequest.split(" ");
        }
        List<String> listOfTerms = Arrays.asList(siteRemoved);
        for (String term : listOfTerms) {
            if (term.startsWith("\"") && term.endsWith("\"")) {
                quoteFlag = true;
                corrected = term.replace("\"", "");
                if (corrected.startsWith("~")) {
                    tildeFlag = true;
                    corrected = corrected.replace("~", "");
                }
            } else if (term.startsWith("~")) {
                tildeFlag = true;
                corrected = term.replace("~", "");
            } else {
                corrected = term;
            }
            String lev_check = "SELECT * FROM t_dictionary WHERE plainword = '" + corrected + "'";
            ResultSet rs = stmt.executeQuery(lev_check);
            //if the term does not exist in DB, check for typo
            while (!rs.isBeforeFirst()) {
                containingTerms.add(term);
                if (tildeFlag == true && quoteFlag == true)
                    corrected = addTilde.concat("\"").concat("~").concat(corrected).concat("\"");
                else if (quoteFlag == true)
                    corrected = addQuotes.concat("\"").concat(corrected).concat("\"");
                else if (tildeFlag == true)
                    corrected = addTilde.concat("~").concat(corrected);
                //check for typo
                corrected = typoCorrect(corrected);

                index = listOfTerms.indexOf(term);
                listOfTerms.set(index, corrected);
                break;
            }
            tildeFlag = false;
            quoteFlag = false;
        }
        termsArray = listOfTerms.toArray(new String[0]);
        termsString = convertStringArrayToString(termsArray, " ");
        if (siteFlag == true)
            perfectTermString = termsString.concat(" ").concat("site:").concat(siteValue);
        else
            perfectTermString = termsString;
        System.out.println(perfectTermString);
        stmt.close();
        conn.close();
        return perfectTermString;
    }

    public List<String> getMissingTerms(){
        return containingTerms ;
    }

    private static String convertStringArrayToString(String[] strArr, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String str : strArr)
            sb.append(str).append(delimiter);
        return sb.substring(0, sb.length() - 1);
    }


    /**
     * If user input could not be processed, look for typo and return results with words with least distance from user input
     *
     * @param term
     * @throws Exception
     */
    public String typoCorrect(String term) throws Exception {
        boolean quoteFlag = false;
        boolean tildeFlag = false;
        List<String> typoCorrected = new ArrayList<>();
        Connection conn = DBconnection.getCon(Literals.dbcon, true);
        Statement stmt = conn.createStatement();
        String words;
        if (term.startsWith("site:")) {
            words = term.replace("site:", "");
        } else if (term.startsWith("\"") && term.endsWith("\"")) {
            quoteFlag = true;
            words = term.replaceAll("\"", "");
            if (words.startsWith("~")) {
                tildeFlag = true;
                words = words.replace("~", "");
            }
        } else if (term.startsWith("~")) {
            tildeFlag = true;
            words = term.replace("~", "");
        } else
            words = term;
        String correctedWord = "SELECT DISTINCT plainword,levenshtein('" + words + "',dTerm.plainword ) AS dictTerm FROM t_dictionary AS dTerm WHERE levenshtein('" + words + "',dTerm.plainword ) BETWEEN 1 AND 3 ORDER BY dictTerm LIMIT 1;";
        ResultSet rs = stmt.executeQuery(correctedWord);
        while (rs.next()) {
            if (tildeFlag == true && quoteFlag == true)
                typoCorrected.add("\"" + "~" + rs.getString(1) + "\"");
            else if (quoteFlag == true)
                typoCorrected.add("\"" + rs.getString(1) + "\"");
            else if (tildeFlag == true)
                typoCorrected.add("~" + rs.getString(1));
            else
                typoCorrected.add(rs.getString(1));
        }
        String correctWord = String.join(" ", typoCorrected);
        stmt.close();
        conn.close();
        return correctWord;
    }


    /**
     * look for possible alternatives with word having least distance from user input
     *
     * @param queryRequest
     * @throws Exception
     */
    public String alternativesSuggest(String queryRequest) throws Exception {
        String siteValue = null;
        String removeSite;
        String suggestion;
        String termsString;
        //String suggestedWord;
        String corrected;
        String[] termsArray;
        String[] siteRemoved;
        boolean siteFlag = false;
        boolean quoteFlag = false;
        boolean tildeFlag = false;
        List<String> alternatives = new ArrayList<>();
        SearchReq searchReq = new SearchReq();
        Connection conn = DBconnection.getCon(Literals.dbcon, true);
        Statement stmt = conn.createStatement();
        if (queryRequest.toLowerCase().contains("site:".toLowerCase())) {
            siteFlag = true;
            siteValue = searchReq.processSearchString(queryRequest);
            removeSite = queryRequest.replace("site:", "").replace(siteValue, "");
            siteRemoved = removeSite.split(" ");
        } else {
            siteRemoved = queryRequest.split(" ");
        }
        for (String term : siteRemoved) {
            if (term.startsWith("\"") && term.endsWith("\"")) {
                quoteFlag = true;
                corrected = term.replace("\"", "");
                if (corrected.startsWith("~")) {
                    tildeFlag = true;
                    corrected = corrected.replace("~", "");
                }
            } else if (term.startsWith("~")) {
                tildeFlag = true;
                corrected = term.replace("~", "");
            } else {
                corrected = term;
            }
            corrected = getStemedWord(corrected);
            /*ResultSet rs = stmt.executeQuery("SELECT stemmedword FROM t_dictionary WHERE plainword='" + words + "'");
            while (rs.next())
                stemmedWord = rs.getString(1);
            ResultSet rs_score = stmt.executeQuery("SELECT score FROM features WHERE term='" + stemmedWord + "'");
            while (rs_score.next())
                scoreStemmedWord = rs_score.getDouble(1);*/
            String sugestWords = "SELECT DISTINCT term,levenshtein('" + corrected + "',dTerm.term ) AS dictTerm,score FROM features AS dTerm WHERE levenshtein('" + corrected + "',dTerm.term) BETWEEN 1 AND 5 ORDER BY dictTerm LIMIT 1;";
            ResultSet rs = stmt.executeQuery(sugestWords);
            while (rs.next()) {
                if (tildeFlag == true && quoteFlag == true)
                    alternatives.add("\"" + "~" + rs.getString(1) + "\"");
                else if (quoteFlag == true)
                    alternatives.add("\"" + rs.getString(1) + "\"");
                else if (tildeFlag == true)
                    alternatives.add("~" + rs.getString(1));
                else
                    alternatives.add(rs.getString(1));
                /*suggestedWord = rs.getString(1);
                scoreSuggestedWord = rs_suggest.getDouble(3);
                if (quote_flag == true) {
                    alternatives.add("\"" + suggestedWord + "\"");
                    quote_flag = false;
                } else
                    alternatives.add(suggestedWord);*/
            }
            tildeFlag = false;
            quoteFlag = false;
        }
        termsArray = alternatives.toArray(new String[0]);
        termsString = convertStringArrayToString(termsArray, " ");
        if (siteFlag == true)
            suggestion = termsString.concat(" ").concat("site:").concat(siteValue);
        else
            suggestion = termsString;
        stmt.close();
        conn.close();
        return suggestion;
    }

    private String getStemedWord(String corrected){
        String stemedWord = "";
        try {
            Connection conn = DBconnection.getCon(Literals.dbcon, true);
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT stemmedword FROM t_dictionary WHERE plainword='"+corrected+"'");
            while (resultSet.next())
                stemedWord = resultSet.getString(1);
            stmt.close();
            conn.close();
        }catch (Exception e){
            System.out.println("error");
        }
        return stemedWord;
    }
}
