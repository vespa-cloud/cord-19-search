// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package ai.vespa.example.cord19.searcher;

import ai.vespa.models.evaluation.FunctionEvaluator;
import ai.vespa.models.evaluation.Model;
import ai.vespa.models.evaluation.ModelsEvaluator;
import com.yahoo.language.process.Embedder;
import com.yahoo.language.wordpiece.WordPieceEmbedder;
import com.yahoo.prelude.hitfield.HitField;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.result.Hit;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.search.yql.FieldFilter;
import com.yahoo.tensor.IndexedTensor;

import com.yahoo.tensor.TensorAddress;
import com.yahoo.tensor.TensorType;
import java.util.List;
import java.util.ArrayList;
import com.yahoo.component.chain.dependencies.After;
import com.yahoo.component.chain.dependencies.Provides;
import com.yahoo.component.chain.dependencies.Before;
import com.yahoo.tensor.functions.Reduce;

@Provides("CrossReRanking")
@Before("Deduping")
@After("ExternalYql")
public class ReRankingSearcher extends Searcher {

    private final Model model;
    private WordPieceEmbedder tokenizer;
    private static final String MODEL_NAME = "trec_covid_synthetic";

    private static final int MAX_SEQUENCE_LENGTH = 256;

    protected static class BertModelBatchInput  {
        final IndexedTensor inputIds;
        final IndexedTensor attentionMask;
        final IndexedTensor tokenTypeIds;

        protected BertModelBatchInput(IndexedTensor inputIds, IndexedTensor attentionMask, IndexedTensor tokenTypeIds) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
            this.tokenTypeIds = tokenTypeIds;
        }

        @Override
        public String toString() {
            return inputIds.toString(true,true);
        }
    }

    public ReRankingSearcher(ModelsEvaluator modelsEvaluator, WordPieceEmbedder tokenizer) {
        this.tokenizer = tokenizer;
        this.model = modelsEvaluator.requireModel(MODEL_NAME);
    }

    @Override
    public Result search(Query query, Execution execution) {
        if(!query.properties().getBoolean("cross-rerank",false))
            return execution.search(query);

        int userHits = query.getHits();
        int userOffset = query.getOffset();
        int reRankCount = query.properties().getInteger("cross-rerank-count",20);
        query.setHits(reRankCount);
        query.setOffset(0);
        query.properties().set(FieldFilter.FIELD_FILTER_DISABLE,true);
        if(!query.getPresentation().getSummaryFields().isEmpty()) {
            query.getPresentation().getSummaryFields().add("title");
            query.getPresentation().getSummaryFields().add("abstract");
        }

        Result result = execution.search(query);
        execution.fill(result,"short");
        Result reRanked = reRank(result);
        reRanked.hits().trim(userOffset, userHits);
        int index = 0;
        StringBuilder builder = new StringBuilder();

        for(Hit h: result.hits()) {
            if(h.isAuxiliary())
                continue;
            String id = (String) h.getField("cord_uid");
            builder.append("#: ").append(index).append(", ");
            builder.append(id).append(", score:").append(
                    h.getRelevance().getScore()).append(", filled=").append(h.getFilled()).append("\n");
            index++;
        }
        query.trace("Hits after cross-reranking:\n" + builder.toString(),1);
        return reRanked;
    }

    private Result reRank(Result result) {
        if(result.getConcreteHitCount() == 0)
            return result;
        List<Integer> queryTokens =
                tokenizer.embed(result.getQuery().getModel().getQueryString(),
                        new Embedder.Context("query"));
        BertModelBatchInput input = buildModelInput(queryTokens, result);
        IndexedTensor scores = batchInference(input);
        double min = scores.min("d0").reduce(Reduce.Aggregator.min,"d1").asDouble();
        double max = scores.max("d0").reduce(Reduce.Aggregator.max,"d1").asDouble();
        int index = 0;
        for (Hit hit : result.hits()) {
            if(hit.isAuxiliary())
                continue;
            double score = scores.get(TensorAddress.of(index,0));
            hit.setField("cross-score", score);
            hit.setField("retriever-score", hit.getRelevance().getScore());
            double scaledScore = (score - min)/(max - min);
            double relevance = hit.getRelevance().getScore();
            if(result.getQuery().getRanking().getProfile().contains("hybrid")) {
                hit.setRelevance((relevance + scaledScore) / 2);
            }
            else {
                hit.setRelevance(scaledScore);
            }
            index++;
        }
        result.hits().sort();
        return result;
    }

    private String getHitField(Hit hit, String field) {
        Object f = hit.getField(field);
        if (f == null)
            return "";
        else if (f instanceof String)
            return (String)f;
        else if( f instanceof HitField) {
            HitField dynamicField = (HitField) hit.getField(field);
            if (dynamicField != null) {
                return dynamicField.getContent(" "," ","...");
            }
        }
        return "";
    }

    protected BertModelBatchInput buildModelInput(List<Integer> queryTokens, Result result) {
        List<List<Integer>> batch = new ArrayList<>(result.getHitCount());
        int maxSeenLength = 0;
        for (Hit h: result.hits()) {
            if(h.isAuxiliary())
                continue;
            String title = getHitField(h,"title");
            String text = getHitField(h,"abstract");
            List<Integer> titleTextTokens = new ArrayList<>(128);
            if(title != null) {
                titleTextTokens.addAll(this.tokenizer.embed(title, new Embedder.Context("title")));
            }
            if(text != null) {
                titleTextTokens.addAll(this.tokenizer.embed(text, new Embedder.Context("text")));
            }
            batch.add(titleTextTokens);
            if (titleTextTokens.size() > maxSeenLength)
                maxSeenLength = titleTextTokens.size();
        }

        int sequenceLength = maxSeenLength + queryTokens.size() + 3;
        if (sequenceLength > MAX_SEQUENCE_LENGTH)
            sequenceLength = MAX_SEQUENCE_LENGTH;

        TensorType batchType = new TensorType.Builder(TensorType.Value.FLOAT).
                indexed("d0", result.hits().size()).indexed("d1",sequenceLength).build();
        IndexedTensor.Builder inputIdsBatchBuilder = IndexedTensor.Builder.of(batchType);
        IndexedTensor.Builder attentionMaskBatchBuilder = IndexedTensor.Builder.of(batchType);
        IndexedTensor.Builder tokenTypeIdsBatchBuilder = IndexedTensor.Builder.of(batchType);
        int queryLength = queryTokens.size();
        int batchId = 0;
        for (List<Integer> passage : batch) {
            int[] inputIds = new int[sequenceLength];
            byte[] attentionMask = new byte[sequenceLength];
            byte[] tokenType = new byte[sequenceLength];

            inputIds[0] = 101;
            attentionMask[0] = 1;
            tokenType[0] = 0;
            int index = 1;

            for (int j = 0; j < queryLength;j++) {
                if(index == sequenceLength -2 )
                    break;
                inputIds[index] = queryTokens.get(j);
                attentionMask[index] = 1;
                tokenType[index] = 0;
                index++;
            }
            inputIds[index] = 102;
            attentionMask[index] = 1;
            tokenType[index] = 0;
            index++;
            for (int j = 0; j < passage.size();j++) {
                if(index == sequenceLength -1)
                    break;
                inputIds[index] = passage.get(j);
                attentionMask[index] = 1;
                tokenType[index] = 1;
                index++;
            }
            inputIds[index] = 102;
            attentionMask[index] = 1;
            tokenType[index] = 1;

            for (int k = 0; k < sequenceLength; k++) {
                inputIdsBatchBuilder.cell(inputIds[k], batchId, k);
                attentionMaskBatchBuilder.cell(attentionMask[k], batchId, k);
                tokenTypeIdsBatchBuilder.cell(tokenType[k], batchId, k);
            }
            batchId++;
        }
        return new BertModelBatchInput(inputIdsBatchBuilder.build(),
                attentionMaskBatchBuilder.build(),
                tokenTypeIdsBatchBuilder.build());
    }

    protected IndexedTensor batchInference(BertModelBatchInput input) {
        FunctionEvaluator evaluator = this.model.evaluatorOf();
        IndexedTensor scores = (IndexedTensor) evaluator.bind("input_ids",input.inputIds)
                .bind("attention_mask", input.attentionMask).
                bind("token_type_ids",input.tokenTypeIds).evaluate();
        return scores;
    }
}
