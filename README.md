<!-- Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root. -->

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://assets.vespa.ai/logos/Vespa-logo-green-RGB.svg">
  <source media="(prefers-color-scheme: light)" srcset="https://assets.vespa.ai/logos/Vespa-logo-dark-RGB.svg">
  <img alt="#Vespa" width="200" src="https://assets.vespa.ai/logos/Vespa-logo-dark-RGB.svg" style="margin-bottom: 25px;">
</picture>

# Vespa Cloud sample applications - CORD-19 Search

Vespa application creating an index of the [CORD-19](https://allenai.org/data/cord-19) dataset.

* Frontend repository: [https://github.com/vespa-engine/cord-19](https://github.com/vespa-engine/cord-19)
* [API doc](https://github.com/vespa-engine/cord-19/blob/master/cord-19-queries.md)
* Deploy a copy of this application by cloning this repo, and deploy it to 
  [Vespa Cloud](https://cloud.vespa.ai/) or [on your own](experiment-yourself.md).

## Demonstrated Vespa features


* Accelerated retrieval using [weakAnd](https://docs.vespa.ai/en/using-wand-with-vespa.html)
* Custom [ranking](https://docs.vespa.ai/en/ranking.html), both [BM25](https://docs.vespa.ai/en/reference/bm25.html), and advanced neural [ColBERT](https://blog.vespa.ai/pretrained-transformer-language-models-for-search-part-3/)
* Document and query time [inference](https://docs.vespa.ai/en/stateless-model-evaluation.html) using language models (ColBERT)
* [Vespa Grouping](https://docs.vespa.ai/en/grouping) to allow users to drill down into the result set 
* Vespa [approximate nearest neighbor search](https://docs.vespa.ai/en/approximate-nn-hnsw.html) for similar articles functionality
* Vespa de-duping, using model inference to compute N * N document to document similarity, at query time to eliminate near duplicates


<pre data-test="exec" data-test-assert-contains='"totalCount":1'>
$ curl "&lt;endpoint&gt;/search/?query=sars-cov-2"
</pre>
