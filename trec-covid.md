# TREC-COVID 

Export the documents using [ir_datasets](https://ir-datasets.com/). Note that there are two trec-covid datasets:

- [beir/trec-covid](https://ir-datasets.com/beir.html#beir/trec-covid) 
- [cord19/trec-covid](https://ir-datasets.com/cord19.html#cord19/trec-covid)

There is differences in the total number of documents and the number of relevance judgments. In this work we use
the `beir/trec-covid` version. 

```
ir_datasets export beir/trec-covid docs --format jsonl --fields text title doc_id  |python3 scripts/trec-covid-dataset.py > trec_covid_feed.jsonl
```

Index the dataset into Vespa 

```
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file trec_covid_feed.jsonl --verbose
```

## Evaluation 

Dump query-document relevance judgements in [trec_eval](https://github.com/usnistgov/trec_eval) format using [ir_datasets](https://ir-datasets.com/trec_eval.html):

```
ir_datasets export beir/trec-covid qrels > beir-trec-covid-qrels.txt
```

### Create ranking runs 

```
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25                    
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert
```

### Evaluate 
Install `trec_eval`:

```
git clone https://github.com/usnistgov/trec_eval.git && cd trec_eval
make install
```

#### BM25 
```
trec_eval -mndcg_cut.10 beir-trec-covid-qrels.txt bm25.run                         
ndcg_cut_10           	all	0.6826
```

#### ColBERT
```
trec_eval -mndcg_cut.10 beir-trec-covid-qrels.txt colbert.run                         
ndcg_cut_10           	all	0.6583
```

#### Hybrid BM25 + ColBERT
```
trec_eval -mndcg_cut.10 beir-trec-covid-qrels.txt colbert.run                         
ndcg_cut_10           	all	0.7426
```