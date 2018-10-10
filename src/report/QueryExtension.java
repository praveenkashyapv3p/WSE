package report;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryExtension {
    public static List<String> getsynengwordnet(String word) {
        List<String> synonyms = new ArrayList<>();

        try {
            URL urldictionary = new URL("file", null, "F:\\Sem 3\\Group-08WSE\\HawkHunt\\src\\WordNet-3.0\\WordNet-3.0\\dict");
            IDictionary dictionary = new Dictionary(urldictionary);
            dictionary.open();
            if (word.length() > 0) {
                for (edu.mit.jwi.item.POS pos : edu.mit.jwi.item.POS.values()) {
                    IIndexWord idxWord = dictionary.getIndexWord(word, pos);
                    if (idxWord != null) {
                        for (IWordID wordId : idxWord.getWordIDs()) {
                            IWord aWord = dictionary.getWord(wordId);
                            ISynset synset = aWord.getSynset();
                            for (IWord w : synset.getWords()) {
                                synonyms.add(w.getLemma().replaceAll("_", " ").trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(synonyms);
        return synonyms;
    }

    public static List<String> getsyngermanwordnet(String synonymWord) {
        String lineOfWords;
        String bracketRemoval;
        String synonyms = "";
        List<String> germanSynonyms = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("F:\\Sem 3\\Group-08WSE\\HawkHunt\\src\\openthesaurus.txt"));
            while((lineOfWords = br.readLine()) != null) {
                if (lineOfWords.toLowerCase().startsWith(synonymWord)) {
                    synonyms = lineOfWords;
                }
                List<String> synonymList = new ArrayList<>(Arrays.asList(synonyms.split(";")));
                for (String term : synonymList) {
                    while (term.contains("(")) {
                        bracketRemoval = term.substring(term.indexOf("("), term.indexOf(")") + 1);
                        term = term.replace(bracketRemoval, "").trim();
                    }
                    germanSynonyms.add(term);
                    //System.out.println(term);
                }
            }
        }catch (Exception e){}
        return germanSynonyms;
    }
}
