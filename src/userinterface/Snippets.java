package userinterface;

import db.DBconnection;
import literals.Literals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class Snippets extends Literals {
    int threshold = 4;

    public String snippet(int docid, String urls, String userInput) throws Exception {
        String retrievedContent = "";
        String snippets = "";
        int index = 0;
        int x;
        String title = "";
        System.out.println("docid=" + docid + "\nurl=" + urls + "\nterms=" + userInput);
        List<String> queryList = new ArrayList<>(Arrays.asList(userInput.split(" ")));
        queryList = snippetQuery(queryList, docid);
        String content = "SELECT content FROM documents WHERE docid=" + docid + ";";
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        ResultSet resultSet = stmt.executeQuery(content);
        while (resultSet.next()) {
            retrievedContent = resultSet.getString(1);
        }
        List<String> contentList = new ArrayList<>(Arrays.asList(retrievedContent.split(" ")));
        int snplength = 0;
        List<String> finalList = new ArrayList<>();
        System.out.println("queryList :" + queryList);
        for (String term : queryList) {
            if (contentList.contains(term)) {
                finalList.add(term);
            }
        }
        int[] tracker = new int[finalList.size()];
        Arrays.fill(tracker, 0);
        System.out.println("final list :" + finalList);
        int i = 0;
        for (String term : finalList) {
            if (snplength < snippet_length) {
                if (!(snippets.contains(term))) {
                    index = contentList.indexOf(term);
//                    System.out.println("term :"+term);
//                    System.out.println("index :"+index);
//                    System.out.println("content list size :"+contentList.size());
                    if (index < threshold) {
                        if (contentList.size() < (index + threshold)) {
//                            System.out.println("loop1");
                            snippets = snippets.concat(contentList.subList(1, contentList.size() - 1).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" ")));
                        } else {
//                            System.out.println("loop1 else");
                            snippets = snippets.concat(contentList.subList(1, index + threshold).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" "))).concat("...");
                        }
                        tracker[i] = 1;
                    } else if (index > contentList.size() - threshold) {
//                        System.out.println("loop2");
                        if (snippets.endsWith("..."))
                            snippets = snippets.concat(contentList.subList(index - threshold, contentList.size() - 1).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" ")));
                        else
                            snippets = snippets.concat("...").concat(contentList.subList(index - threshold, contentList.size() - 1).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" ")));
                        tracker[i] = 1;
                    } else {
//                        System.out.println("loop3");
                        if (snippets.endsWith("..."))
                            snippets = snippets.concat(contentList.subList(index - threshold, index + threshold).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" "))).concat("...");
                        else
                            snippets = snippets.concat("...").concat(contentList.subList(index - threshold, index + threshold).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" "))).concat("...");
                        tracker[i] = 1;
                    }
                } else {
//                    System.out.println("loop4");
                    tracker[i] = 1;
                }
            } else
                break;
            snplength = Arrays.asList(snippets.split(" ")).size();
            i++;
//            System.out.println("snippet for:"+i+ " is:"+snippets);
        }
        List<String> snippetList = new ArrayList<>(Arrays.asList(snippets.split(" ")));
        if (snippetList.size() < snippet_length) {
            int diff = snippet_length - snippetList.size();
            int diff_threshold = diff / 2;
            System.out.println("diff_threshold :" + diff_threshold);
            int first_index = contentList.indexOf(snippetList.get(0));
            int last_index = contentList.indexOf(snippetList.get(snippetList.size() - 1));
            int startlimit = 0;
            int endlimit = 0;
            if (first_index != 0) {
                if ((first_index - (diff_threshold)) < 0) {
                    int endcal = last_index + (diff_threshold) + ((diff_threshold + 1) - first_index);
                    if (endcal < contentList.size()) {
                        endlimit = endcal;
                    } else {
                        endlimit = contentList.size();
                    }
                } else {
                    startlimit = first_index - (diff_threshold);
                    int endcal = last_index + (diff_threshold);
                    if (endcal < contentList.size())
                        endlimit = endcal;
                    else {
                        endlimit = contentList.size();
                        int startval = first_index - (diff_threshold + 1) - (endcal - contentList.size());
                        if (startval > 0)
                            startlimit = startval;
                        else
                            startlimit = 0;
                    }
                }
            } else {
                int endcal = last_index + (diff);
                if (endcal < contentList.size()) {
                    endlimit = endcal;
                } else {
                    endlimit = contentList.size();
                }

            }
            System.out.println("first_index :" + first_index);
            if (first_index != 0 && first_index != -1) {
                snippets = contentList.subList(startlimit, first_index - 1).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" ")).concat(" ").concat(snippets);
            }
            snippets = snippets.concat(" ").concat(contentList.subList(last_index + 1, endlimit).stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" ")));

        }


        System.out.println("tracker :" + Arrays.toString(tracker));
        System.out.println("snippet :" + snippets);
        System.out.println("snippet size :" + Arrays.asList(snippets.split(" ")).size());
//        System.out.println("index=" + index +"\ttitle="+title+ "\tstring=" + snippets);
        stmt.close();
        conn.close();
        return snippets;
    }

    private List<String> snippetQuery(List<String> queryList, int docid) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        Map<String, Integer> snippetQuery = new HashMap<>();
        for (String query : queryList) {
            String snippetQueryString = "SELECT term,term_frequency FROM features WHERE docid=" + docid + " AND lower(term)='" + query.toLowerCase() + "'";
            ResultSet rs = stmt.executeQuery(snippetQueryString);
            while (rs.next()) {
                snippetQuery.put(rs.getString(1), rs.getInt(2));
            }
        }
        List<String> sortedValueList = new ArrayList<>();
        List<String> unstemmedsortedValueList = new ArrayList<>();
        System.out.println("query list :" + snippetQuery);
        snippetQuery = snippetQuery.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        sortedValueList = snippetQuery.keySet().stream().collect(Collectors.toList());
        for (String val : sortedValueList) {
            String qs = "SELECT distinct plainword FROM t_dictionary WHERE stemmedword='" + val + "'";
            System.out.println("query :" + qs);
            ResultSet rs = stmt.executeQuery(qs);
            while (rs.next()) {
                System.out.println("val list :" + val);
                unstemmedsortedValueList.add(rs.getString(1));
            }
        }
        System.out.println("sorted list :" + sortedValueList);
        System.out.println("unstemmedsorted list :" + unstemmedsortedValueList);
        stmt.close();
        conn.close();
        return unstemmedsortedValueList;
    }
}