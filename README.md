<!-- Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root. -->

![Vespa Cloud logo](https://cloud.vespa.ai/assets/logos/vespa-cloud-logo-full-black.png)

# Vespa Cloud sample applications - CORD-19 Search

Vespa application creating an index of the [CORD-19](https://allenai.org/data/cord-19) dataset.

* Frontend: [https://cord19.vespa.ai/](https://cord19.vespa.ai/)
* API: [https://api.cord19.vespa.ai/search/?query=sars-cov-2](https://api.cord19.vespa.ai/search/?query=sars-cov-2) -
  [API doc](https://github.com/vespa-engine/cord-19/blob/master/cord-19-queries.md)
* Deploy a copy of this application by cloning this repo, and deploy it to 
  [Vespa Cloud](https://cloud.vespa.ai/) or [on your own](experiment-yourself.md).

[![Continuous deployment to the Vespa Cloud](https://github.com/vespa-engine/sample-apps/workflows/Deploy%20the%20Vespa%20CORD-19%20search%20application%20to%20Vespa%20Cloud/badge.svg)](.github/workflows/deploy-vespa-cord-19-search.yaml)

## Demonstrated Vespa features


* Accelerated retrieval using [weakAnd](https://docs.vespa.ai/en/using-wand-with-vespa.html)
* Custom [ranking](https://docs.vespa.ai/en/ranking.html), both [BM25](https://docs.vespa.ai/en/reference/bm25.html), and advanced neural [ColBERT](https://blog.vespa.ai/pretrained-transformer-language-models-for-search-part-3/)
* Document and query time [inference](https://docs.vespa.ai/en/stateless-model-evaluation.html) using language models (ColBERT)
* [Vespa Grouping](https://docs.vespa.ai/en/grouping) to allow users to drill down into the result set 
* Vespa [approximate nearest neighbor search](https://docs.vespa.ai/en/approximate-nn-hnsw.html) for similar articles functionality
* Vespa de-duping, using model inference to compute N * N document to document similarity, at query time to eliminate near duplicates


<pre data-test="exec" data-test-assert-contains='"totalCount":1'>
$ curl "https://api.cord19.vespa.ai/search/?query=sars-cov-2"
</pre>

<!-- Moved from https://github.com/vespa-engine/sample-apps/blob/a5a5fea369554691ef61d3e26c9337c878a0b2ea/vespa-cloud/cord-19-search/README.md -->
