package ai.vespa.example.cord19.searcher;

import ai.vespa.models.evaluation.FunctionEvaluator;
import ai.vespa.models.evaluation.ModelsEvaluator;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.result.Hit;
import com.yahoo.search.result.HitGroup;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.tensor.IndexedTensor;
import com.yahoo.tensor.TensorType;

import java.util.Arrays;
import java.util.Set;

import com.yahoo.component.chain.dependencies.After;

@After("ExternalYql")
public class DeDupingSearcher extends Searcher {
    private final ModelsEvaluator modelsEvaluator;

    private static final String summary = "embeddings";
    private static final String vectorField = "specter_embedding";
    private static int dim = 768;

    public DeDupingSearcher(ModelsEvaluator evaluator) {
        this.modelsEvaluator = evaluator;
    }

    @Override
    public Result search(Query query, Execution execution) {
        if (!query.properties().getBoolean("collapse.enable", false))
            return execution.search(query);

        int userHits = query.getHits();
        query.setHits(100);
        Result result = execution.search(query);
        ensureFilled(result, summary, execution);
        result = dedup(result, userHits);
        result.hits().trim(query.getOffset(), userHits);
        result.hits().forEach(h -> h.removeField(vectorField));
        query.getPresentation().getSummaryFields().remove(vectorField);
        return result;
    }

    /**
     * Deduping based on vector similarity
     *
     * @param result   the result to dedupe
     * @param userHits the number of hits requested by the user
     * @return
     */

    public Result dedup(Result result, int userHits) {
        if (result.getTotalHitCount() == 0 || result.hits().getError() != null)
            return result;

        double similarityThreshold = result.getQuery().properties().
                getDouble("collapse.similarity.threshold", 0.90);

        int maxHits = result.getQuery().properties().
                getInteger("collapse.similarity.max-hits", 100);

        int size = Math.min(result.getHitCount(), maxHits);
        //Iterate over the diagonal and for
        //each hit see if we already added
        //a hit with high similarity to the current image i
        IndexedTensor similarityMatrix = getSimilarityMatrix(result, size);
        HitGroup uniqueHits = new HitGroup();
        for (int i = 0; i < size; i++) {
            double maxSim = 0;
            for (int j = i - 1; j >= 0; j--) {
                float sim = similarityMatrix.getFloat(i, j);
                if (sim > maxSim)
                    maxSim = sim;
            }
            if (maxSim < similarityThreshold) {
                uniqueHits.add(result.hits().get(i));
                if (uniqueHits.size() == userHits)
                    break;
            }
        }
        result.setHits(uniqueHits);
        return result;
    }

    public IndexedTensor getSimilarityMatrix(Result result, int size) {
        TensorType type = new TensorType.Builder(TensorType.Value.FLOAT).
                indexed("d0", size).indexed("d1", dim).build();
        IndexedTensor.Builder builder = IndexedTensor.Builder.of(type);
        HitGroup hits = result.hits();
        for (int i = 0; i < size; i++) {
            IndexedTensor vector = (IndexedTensor) hits.get(i).getField(vectorField);
            for (int j = 0; j < vector.size(); j++)
                builder.cell(vector.get(j), i, j);
        }
        IndexedTensor batch = builder.build();
        result.getQuery().trace("Input tensor shape " + Arrays.asList(batch.shape()), 3);
        // Perform N X N similarity
        FunctionEvaluator similarity = modelsEvaluator.
                evaluatorOf("vespa_pairwise_similarity");
        return (IndexedTensor) similarity.bind(
                "documents", batch).evaluate();
    }
}

