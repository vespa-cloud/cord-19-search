<!-- Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root. -->

![Vespa logo](https://vespa.ai/assets/vespa-logo-color.png)

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
the current directory. 

```
tar xzvf cord_19_embeddings.tar.gz
cat cord_19_embeddings_2022-06-02.csv| python3 /path/to/app/scripts/merge.py > merged-feed.jsonl
```

## Feed the data
Use [vespa-feed-client](https://docs.vespa.ai/en/vespa-feed-client.html) to feed the data to your Vespa instance
```
vespa-feed-client --file merged-feed.jsonl  --endpoint <endpoint-url>  --verbose
```
