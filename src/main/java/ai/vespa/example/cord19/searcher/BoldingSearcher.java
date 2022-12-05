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
            if (RelatedArticlesByWeakAndSearcher.stopwords.contains(word))
                item.setFilter(true);
        }
        else if (item instanceof CompositeItem && !(item instanceof PhraseItem)) {
            CompositeItem cItem = (CompositeItem)item;
            for (Item i : cItem.items())
                setFilterForStopWords(i);
        }
    }
}
