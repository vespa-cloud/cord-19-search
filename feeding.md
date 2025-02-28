<!-- Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root. -->

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://assets.vespa.ai/logos/Vespa-logo-green-RGB.svg">
  <source media="(prefers-color-scheme: light)" srcset="https://assets.vespa.ai/logos/Vespa-logo-dark-RGB.svg">
  <img alt="#Vespa" width="200" src="https://assets.vespa.ai/logos/Vespa-logo-dark-RGB.svg" style="margin-bottom: 25px;">
</picture>

## Prerequisites
Python3 installed and two packages to parse and produce Vespa json feed
files. 

```
pip3 install pandas numpy 
```

## Download the CORD-19 dataset 
Download the dataset from [ai2-semanticscholar-cord-19](https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/historical_releases.html).

```
wget https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/historical_releases/cord-19_2022-06-02.tar.gz
tar xzvf cord-19_2022-06-02.tar.gz && cd 2022-06-02
```

## Process the dataset
```
python3 /path/to/app/scripts/convert-to-json.py metadata.csv > feed.jsonl
```
Merge feed file with cord-19 specter embedding. This step expects a `feed.jsonl` file in
the current directory (The file generated by the above run).

```
tar xzvf cord_19_embeddings.tar.gz
cat cord_19_embeddings_2022-06-02.csv| python3 /path/to/app/scripts/merge.py > merged-feed.jsonl
```

## Feed the data
Use the [vespa CLI](https://docs.vespa.ai/en/vespa-cli.html#documents) to feed the data to your Vespa instance:
```
vespa feed -t <endpoint-url> merged-feed.jsonl
```
Indexing is CPU intensive as both abstract and title is encoded using ColBERT. 
