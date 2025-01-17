// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package ai.vespa.example.cord19.searcher;

import com.yahoo.prelude.query.AndItem;
import com.yahoo.prelude.query.CompositeItem;
import com.yahoo.prelude.query.Item;
import com.yahoo.prelude.query.NearestNeighborItem;
import com.yahoo.prelude.query.TermItem;
import com.yahoo.prelude.query.WordItem;
import com.yahoo.prelude.query.NullItem;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.result.Hit;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.tensor.Tensor;

/**
 * @author jobergum
 */
public class RelatedArticlesByNNSearcher extends RelatedArticlesSearcher {

    private static final String embeddingSummary = "embeddings";

    @Override
    protected void addRelatedItem(Integer relatedArticleId, Execution execution, Query query) {
        Article article = fetchArticle(relatedArticleId, execution, query);
        addANNItem(article, query);
    }

    private Article fetchArticle(Integer id, Execution execution, Query query) {
        Query articleQuery = new Query();
        query.attachContext(articleQuery);
        articleQuery.getPresentation().setSummary(embeddingSummary);
        WordItem idFilter = new WordItem(id.toString(), "id", true);
        articleQuery.getModel().getQueryTree().setRoot(idFilter);
        articleQuery.getModel().setRestrict("doc");
        articleQuery.setHits(1);
        articleQuery.getRanking().setProfile("unranked");
        Result articleResult = execution.search(articleQuery);
        execution.fill(articleResult, embeddingSummary);
        return articleFrom(articleResult);
    }

    private Article articleFrom(Result result) {
        if (result.hits().size() < 1)
            throw new IllegalArgumentException("Requested article not found");
        Hit articleHit = result.hits().get(0);
        return new Article((Tensor) articleHit.getField("specter_embedding"));
    }

    /**
     * Adds a term to the given query to find related articles
     * @param article         the article to fetch related articles for
     */

    private void addANNItem(Article article, Query query) {
        Item nnRoot;
        String rankProfile = "related-specter";
        nnRoot = createNNItem("specter_embedding", "specter_vector");
        query.getRanking().getFeatures().put("query(specter_vector)", article.specterEmbedding);
        filter(rankProfile, query, nnRoot);
    }

    private void filter(String rankprofile, Query query, Item nn) {
        Item root = query.getModel().getQueryTree().getRoot();
        if (!hasTextTerms(root)) {
            query.getRanking().setProfile(rankprofile);
        }
        if (!(root instanceof NullItem || root == null)) {
            AndItem andItem = new AndItem();
            andItem.addItem(root);
            andItem.addItem(nn);
            query.getModel().getQueryTree().setRoot(andItem);
        } else {
            query.getModel().getQueryTree().setRoot(nn);
        }
    }

    private NearestNeighborItem createNNItem(String field, String query) {
        NearestNeighborItem nn = new NearestNeighborItem(field, query);
        nn.setAllowApproximate(true);
        nn.setTargetNumHits(100);
        return nn;
    }

    private boolean hasTextTerms(Item item) {
        if (item instanceof CompositeItem) {
            for (Item child : ((CompositeItem) item).items())
                if (hasTextTerms(child))
                    return true;
        }
        return (item instanceof TermItem) && !item.isFilter();
    }

    private static class Article {
        final Tensor specterEmbedding;
        Article(Tensor specterEmbedding) {
            this.specterEmbedding = specterEmbedding;
        }
    }

}
