// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package ai.vespa.example.cord19.searcher;

import ai.vespa.example.cord19.colbert.ColbertConfig;
import ai.vespa.models.evaluation.Model;
import ai.vespa.models.evaluation.ModelsEvaluator;
import com.google.inject.Inject;
import com.yahoo.language.process.Embedder;
import com.yahoo.language.wordpiece.WordPieceEmbedder;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.Searcher;
import com.yahoo.search.searchchain.Execution;
import com.yahoo.tensor.Tensor;
import com.yahoo.tensor.TensorAddress;
import com.yahoo.tensor.TensorType;
import com.yahoo.tensor.functions.Reduce;

import java.util.ArrayList;
import java.util.List;

/**
 * Searcher which produces the ColBERT query embedding tensor
 * tensor(qt{}, x[32]) by using Vespa's model inference support.
 */
public class ColBERTSearcher extends Searcher {

    private final TensorType colbertTensorType;
    private final int query_max_length;
    private final int dim;
    private final Model colbertModel;

    private final WordPieceEmbedder embedder;

    private static final Tensor BATCH_TENSOR = Tensor.from("tensor<float>(d0[1]):[1]");
    private static final String modelName = "colbert_encoder";


    @Inject
    public ColBERTSearcher(ColbertConfig config, ModelsEvaluator evaluator, WordPieceEmbedder embedder)  {
        this.query_max_length = config.max_query_length();
        this.dim = config.dim();
        this.colbertModel = evaluator.requireModel(modelName);
        this.embedder = embedder;
        this.colbertTensorType = new TensorType.Builder(TensorType.Value.FLOAT).
                mapped("qt").indexed("x",dim).build();
    }

    @Override
    public Result search(Query query, Execution execution) {
        if(query.getRanking().hasRankProfile() &&
                query.getRanking().getProfile().contains("colbert")) {

            String queryInput = query.getModel().getQueryString();
            List<Integer> bertTokenIds = this.embedder.embed(queryInput, new Embedder.Context("q"));

            if(bertTokenIds.size() >query_max_length)
                bertTokenIds = bertTokenIds.subList(0,query_max_length);
            TensorInput queryTensorInput = new TensorInput(bertTokenIds);
            TensorInput.setTo(query.properties(), queryTensorInput);
            Tensor colBertTensor = getColBertTensor(query);
            colBertTensor = rewriteTensor(colBertTensor);
            query.getRanking().getFeatures().put("query(qt)", colBertTensor);
        }
        return execution.search(query);
    }

    protected Tensor getColBertTensor(Query originalQuery)  {
        int CLS_TOKEN_ID = 101;  // [CLS]
        int SEP_TOKEN_ID = 102;  // [SEP]
        int MASK_TOKEN_ID = 103; // [MASK]
        int Q_TOKEN_ID = 1; // [unused0] token id used during training to differentiate query versus document.

        TensorInput helper = TensorInput.getFrom(originalQuery.properties());
        List<Integer> tokenIds = helper.getQueryTokenIds();
        if(tokenIds.size() > query_max_length -3)
            tokenIds = tokenIds.subList(0,query_max_length-3);

        List<Integer> input_ids = new ArrayList<>(query_max_length);
        List<Integer> attention_mask = new ArrayList<>(query_max_length);

        input_ids.add(CLS_TOKEN_ID);
        input_ids.add(Q_TOKEN_ID);
        input_ids.addAll(tokenIds);

        input_ids.add(SEP_TOKEN_ID);
        int length = input_ids.size();

        // Pad up to max length with mask token_id
        int padding = query_max_length - length;
        for (int i = 0; i < padding; i++)
            input_ids.add(MASK_TOKEN_ID);
        for (int i = 0; i < length; i++)
            attention_mask.add(1);
        for (int i = 0; i < padding; i++)
            attention_mask.add(0);

        Tensor input_ids_batch = TensorInput.getTensorRepresentation(input_ids,"d1").
                multiply(BATCH_TENSOR);
        Tensor attention_mask_batch = TensorInput.getTensorRepresentation(attention_mask,"d1").
                multiply(BATCH_TENSOR);

        return this.colbertModel.evaluatorOf().
                bind("input_ids",input_ids_batch).
                bind("attention_mask",attention_mask_batch).evaluate();
    }

    /**
     * Removes the batch dimension ("d0") from batched output and converts
     * from dense-dense representation to mapped sparse dense
     * @param embedding the Tensor from query encoding with batch dimension
     */
    private Tensor rewriteTensor(Tensor embedding) {
        Tensor t = embedding.reduce(Reduce.Aggregator.min, "d0");
        Tensor.Builder builder = Tensor.Builder.of(colbertTensorType);
        for (int i = 0; i < query_max_length; i++)
            for (int j = 0; j < dim; j++)
                builder.cell(TensorAddress.of(i, j), t.get(TensorAddress.of(i, j)));
        return builder.build();
    }
}
