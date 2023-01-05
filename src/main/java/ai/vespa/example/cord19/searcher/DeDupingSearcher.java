package ai.vespa.example.cord19.searcher;

import ai.vespa.models.evaluation.FunctionEvaluator;
import ai.vespa.models.evaluation.ModelsEvaluator;
import com.yahoo.component.chain.dependencies.Before;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.result.Hit;
import com.yahoo.search.result.HitGroup;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.tensor.IndexedTensor;
import com.yahoo.tensor.TensorType;
import com.yahoo.component.chain.dependencies.After;

@After("ExternalYql")
@Before("ReRanking")
public class DeDupingSearcher extends Searcher {
    private final ModelsEvaluator modelsEvaluator;
    private static final String summary = "embeddings";
    private static final String vectorField = "specter_embedding";
    private static int DIM = 768;

    public DeDupingSearcher(ModelsEvaluator evaluator) {
        this.modelsEvaluator = evaluator;
    }

    @Override
    public Result search(Query query, Execution execution) {
        if (!query.properties().getBoolean("collapse.enable", false))
            return execution.search(query);

        query.getPresentation().getSummaryFields().add(vectorField);
        int userHits = query.getHits();
        int userOffset = query.getOffset();
        query.setHits(100);
        query.setOffset(0);
        Result result = execution.search(query);
        ensureFilled(result, summary, execution);
        result = dedup(result);
        result.hits().trim(userOffset, userHits);
        result.hits().forEach(h -> h.removeField(vectorField));
        query.getPresentation().getSummaryFields().remove(vectorField);
        return result;
    }

    /**
     * Deduping using vector similarity
     * @param result the result to remove near duplicates for
     * @return
     */

    public Result dedup(Result result) {
        if (result.getTotalHitCount() == 0 || result.hits().getError() != null)
            return result;

        double similarityThreshold = result.getQuery().properties().
                getDouble("collapse.similarity.threshold", 0.90);

        int maxHits = result.getQuery().properties().
                getInteger("collapse.similarity.max-hits", 100);

        int size = Math.min(result.hits().getConcreteSizeShallow(), maxHits);

        HitGroup concreteHits = new HitGroup();
        HitGroup auxiliary = new HitGroup();
        for(Hit h: result.hits())
            if(h.isAuxiliary() || h.isMeta())
                auxiliary.add(h);
            else
                concreteHits.add(h);

        IndexedTensor similarityMatrix = getSimilarityMatrix(concreteHits, size);
        HitGroup uniqueHits = new HitGroup();

        //Iterate over the diagonal and for
        //each hit see if we already added
        //a hit with high similarity to the current image
        for (int i = 0; i < concreteHits.size(); i++) {
            double maxSim = 0;
            for (int j = i - 1; j >= 0; j--) {
                float sim = similarityMatrix.getFloat(i, j);
                if (sim > maxSim)
                    maxSim = sim;
            }
            if (maxSim < similarityThreshold) {
                uniqueHits.add(concreteHits.get(i));
            }
        }
        //add the auxiliary hits
        for(Hit h: auxiliary)
            uniqueHits.add(h);
        result.setHits(uniqueHits);
        return result;
    }

    public IndexedTensor getSimilarityMatrix(HitGroup hits, int size) {
        TensorType type = new TensorType.Builder(TensorType.Value.FLOAT).
                indexed("d0", size).indexed("d1", DIM).build();
        IndexedTensor.Builder builder = IndexedTensor.Builder.of(type);
        int index = 0;
        for (Hit h: hits) {
            IndexedTensor vector = (IndexedTensor) h.getField(vectorField);
            for (int j = 0; j < vector.size(); j++)
                builder.cell(vector.get(j), index, j);
            index++;
        }
        IndexedTensor batch = builder.build();
        // Perform N X N similarity
        FunctionEvaluator similarity = modelsEvaluator.
                evaluatorOf("vespa_pairwise_similarity");
        return (IndexedTensor) similarity.bind(
                "documents", batch).evaluate();
    }
}

