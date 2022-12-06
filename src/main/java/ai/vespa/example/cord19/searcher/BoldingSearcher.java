// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package ai.vespa.example.cord19.searcher;

import com.yahoo.prelude.query.CompositeItem;
import com.yahoo.prelude.query.Item;
import com.yahoo.prelude.query.PhraseItem;
import com.yahoo.prelude.query.WordItem;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.searchchain.Execution;

import java.util.Set;

/**
 * This searcher traverse the query tree and looks for known stop word.
 * The stop words are annotated with filter which will avoid
 * bolding them in the search result
 */

public class BoldingSearcher extends Searcher {

    @Override
    public Result search(Query query, Execution execution) {
        setFilterForStopWords(query.getModel().getQueryTree().getRoot());
        return execution.search(query);
    }

    private void setFilterForStopWords(Item item) {
        if (item instanceof WordItem) {
            String word = ((WordItem) item).getWord();
            word = word.toLowerCase();
            if (stopWords.contains(word))
                item.setFilter(true);
        }
        else if (item instanceof CompositeItem && !(item instanceof PhraseItem)) {
            CompositeItem cItem = (CompositeItem)item;
            for (Item i : cItem.items())
                setFilterForStopWords(i);
        }
    }
    protected static Set<String> stopWords = Set.of(
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers",
            "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom",
            "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while",
            "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above",
            "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there",
            "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not",
            "only", "own", "same", "so", "than", "too", "very", "can", "will", "just", "don", "should", "now");
}
