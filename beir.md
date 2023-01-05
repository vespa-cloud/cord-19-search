# Reproducing Vespa ranking runs on the BEIR datasets 

This document demonstrates how to reproduce zero-shot ranking runs on the BEIR benchmark using Vespa.

Go through [experimenting yourself guide](experiment-yourself.md) first on how to build and deploy the app.

If you want to index and experiment with more than one dataset, you should remove the index in between
dataset experiments, this can be accomplished by running the following command:

```
docker exec cord19 bash -c "/opt/vespa/bin/vespa-stop-services; /opt/vespa/bin/vespa-remove-index -force; /opt/vespa/bin/vespa-start-services" 
vespa status --wait 300
```

----

## beir/msmarco
```
ir_datasets export beir/msmarco docs --format jsonl --fields text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets export beir/msmarco qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/msmarco/dev                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/msmarco/dev
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/msmarco/dev

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/trec-covid  
```
ir_datasets export beir/trec-covid docs --format jsonl --fields text title doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets export beir/trec-covid qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/trec-covid                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/trec-covid
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/trec-covid

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/nfcorpus 
```
ir_datasets export beir/nfcorpus docs --format jsonl --fields text title doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets export beir/nfcorpus/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/nfcorpus/test                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/nfcorpus/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/nfcorpus/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/quora
```
ir_datasets export beir/quora docs --format jsonl --fields text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets export beir/quora/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/quora/test                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/quora/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/quora/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/arguana
```
ir_datasets export beir/arguana  docs --format jsonl --fields text title doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets export beir/arguana qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/arguana                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/arguana
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/arguana

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/fiqa
```
ir_datasets export beir/fiqa  docs --format jsonl --fields text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets export beir/fiqa/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/fiqa/test                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/fiqa/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/fiqa/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/scidocs
```
ir_datasets export beir/scidocs  docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/scidocs qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/scidocs                   
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/scidocs
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/scidocs

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/scifact
```
ir_datasets export beir/scifact  docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/scifact/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/scifact/test                  
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/scifact/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/scifact/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/webis-touche2020/v2
```
ir_datasets export beir/webis-touche2020/v2  docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/webis-touche2020/v2 qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/webis-touche2020/v2                  
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/webis-touche2020/v2
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/webis-touche2020/v2

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/nq
```
ir_datasets export beir/nq  docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/nq qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/nq         
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/nq
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/nq

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/fever
```
ir_datasets export beir/fever docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/fever/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/fever/test        
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/fever/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/fever/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/climate-fever
```
ir_datasets export beir/climate-fever docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/climate-fever qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/climate-fever       
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/climate-fever
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/climate-fever

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/hotpotqa
```
ir_datasets export beir/hotpotqa docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/hotpotqa/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/hotpotqa/test       
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/hotpotqa/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/hotpotqa/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```

## beir/dbpedia-entity
```
ir_datasets export beir/dbpedia-entity docs --format jsonl --fields title text doc_id  |python3 scripts/trec-covid-dataset.py > feed.jsonl
./vespa-feed-client-cli/vespa-feed-client --endpoint http://localhost:8080 --file feed.jsonl --verbose

ir_datasets beir/dbpedia-entity/test qrels > qrels 
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking bm25 --dataset beir/dbpedia-entity/test       
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking colbert --dataset beir/dbpedia-entity/test
python3 scripts/evaluate.py --endpoint http://localhost:8080/search/ --ranking hybrid-colbert --dataset beir/dbpedia-entity/test

trec_eval -mndcg_cut.10 qrels bm25.run                         
trec_eval -mndcg_cut.10 qrels colbert.run                         
trec_eval -mndcg_cut.10 qrels.txt hybrid-colbert.run                         
```