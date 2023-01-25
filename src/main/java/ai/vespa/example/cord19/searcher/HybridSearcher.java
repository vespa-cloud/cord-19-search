package ai.vespa.example.cord19.searcher;

import com.yahoo.component.chain.dependencies.After;
import com.yahoo.component.chain.dependencies.Provides;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.result.HitGroup;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.search.result.FeatureData;
import com.yahoo.search.result.Hit;

/**
 * This searcher asks the backend for 2K hits (merged if there are more than one backend content node).
 * The searcher computes the max and min scores for bm25 and colbert_maxsim scores returned
 * using matchfeatures. The scores are then normalized using max-min normalization so that
 * they are in the range 0-1. Finally, the scores are averaged and the 2K hits are re-sorted
 * using this new hybrid score.
 *
 * Using matchfeatures is a cost-efficient way to transfer features calculated by the content nodes
 * to stateless containers.
 *
 */
@After("ExternalYql")
@Provides("HybridReRanking")
public class HybridSearcher extends Searcher {

    private static String MATCH_FEATURES_FIELD = "matchfeatures";
    private static int WINDOW = 2000;

    @Override
    public Result search(Query query, Execution execution) {
        if(!query.getRanking().getProfile().contains("hybrid"))
            return execution.search(query);

        if(!query.properties().getBoolean("fusion.enable", true))
            return execution.search(query);

        if(!query.getPresentation().getSummaryFields().isEmpty()
                && !query.getPresentation().getSummaryFields().contains(MATCH_FEATURES_FIELD)) {
            query.getPresentation().getSummaryFields().add(MATCH_FEATURES_FIELD);
        }

        int hits = query.getHits();
        int offset = query.getOffset();
        query.setHits(WINDOW); //Re-ranking window
        Result result = execution.search(query);
        if(result.getTotalHitCount() == 0
                || result.hits().getErrorHit() != null)
            return result;

        String[] features = query.properties().
                getString("fusion.features", "bm25,colbert_maxsim").split(",");
        normalize(result.hits(),features);
        result.hits().sort();
        result.hits().trim(offset, hits);
        query.setOffset(offset);
        query.setHits((hits));
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for(Hit h: result.hits()) {
            if(h.isAuxiliary())
                continue;
            String id = (String) h.getField("cord_uid");
            builder.append("#: ").append(index).append(", ");
            builder.append(id).append(", score:").append(
                    h.getRelevance().getScore()).append(", filled=").append(h.getFilled()).append("\n");
            index++;
        }
        query.trace("Hits after hybrid fusion:\n" + builder.toString(),1);
        return result;
    }

    /**
     * Implement feature score scaling and normalization
     * @param hits
     * @param features
     */
    void normalize(HitGroup hits, String[] features) {
        // Min - Max normalization
        double[] minValues = new double[features.length];
        double[] maxValues = new double[features.length];
        for(int i = 0; i < features.length;i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
        }

        //Find min and max value in the re-ranking window
        for (Hit hit : hits) {
            if(hit.isAuxiliary())
                continue;
            FeatureData featureData = (FeatureData) hit.getField(MATCH_FEATURES_FIELD);
            if(featureData == null)
                throw new RuntimeException("No feature data in hit - wrong rank profile used?");
            for(int i = 0; i < features.length; i++) {
                double score = featureData.getDouble(features[i]);
                if(score < minValues[i])
                    minValues[i] = score;
                if(score > maxValues[i])
                    maxValues[i] = score;
            }
        }
        //re-score using normalized value
        for (Hit hit : hits) {
            if(hit.isAuxiliary())
                continue;
            FeatureData featureData = (FeatureData) hit.getField(MATCH_FEATURES_FIELD);
            double finalScore = 0;
            for(int i = 0; i < features.length; i++) {
                double score = featureData.getDouble(features[i]);
                finalScore += (score - minValues[i]) / (maxValues[i] - minValues[i]);
            }
            finalScore = finalScore/ features.length;
            hit.setRelevance(finalScore);
        }
    }
}

