<!-- Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root. -->

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://vespa.ai/assets/vespa-ai-logo-heather.svg">
  <source media="(prefers-color-scheme: light)" srcset="https://vespa.ai/assets/vespa-ai-logo-rock.svg">
  <img alt="#Vespa" width="200" src="https://vespa.ai/assets/vespa-ai-logo-rock.svg" style="margin-bottom: 25px;">
</picture>


The Vespa app that powers [CORD-19 Search](https://cord19.vespa.ai/) is 
deployed on [Vespa Cloud](https://cloud.vespa.ai/).

To run the same application on your own laptop, you can use the free and open source
[Vespa container image](https://hub.docker.com/r/vespaengine/vespa/). 

Running the application locally is easy and enables you to play with ranking features, 
see also the [trec-covid relevance reproducing steps](trec-covid.md) for how to evaluate ranking methods 
using the [trec-covid dataset](https://ir.nist.gov/trec-covid/) relevance dataset.

----

All Vespa Cloud applications can be run locally.

Prerequisites:
* [Docker](https://docs.docker.com/engine/installation/) installed
* [Git](https://git-scm.com/downloads) installed
* Operating system: macOS or Linux
* Architecture: x86_64 or arm64
* *Minimum 4 GB* memory dedicated to Docker (the default is 2 GB on Macs).
  Refer to [Docker memory](https://docs.vespa.ai/en/operations/docker-containers.html#memory)
  for details and troubleshooting.
* [Java 17](https://openjdk.org/projects/jdk/17/) installed. 
* [Apache Maven](https://maven.apache.org/install.html).
  This sample app uses custom Java components and Maven is used to build the application.
* zstd: `brew install zstd`

Validate Docker resource settings, should be minimum 4 GB:

<pre>
$ docker info | grep "Total Memory"
</pre>

Install [Vespa CLI](https://docs.vespa.ai/en/vespa-cli.html). 

<pre >
$ brew install vespa-cli
</pre>

Set target env, it's also possible to deploy to [Vespa Cloud](https://cloud.vespa.ai/)
using target cloud. 

For local deployment using docker image use 

<pre data-test="exec">
$ vespa config set target local
</pre>

For cloud deployment using [Vespa Cloud](https://cloud.vespa.ai/) use

<pre>
$ vespa config set target cloud
$ vespa config set application tenant-name.myapp.default
$ vespa auth login 
$ vespa auth cert
</pre>

See also [Cloud Vespa getting started guide](https://cloud.vespa.ai/en/getting-started). It's possible
to switch between local deployment and cloud deployment by changing the `config target`. 

Clone this repo:

<pre data-test="exec">
$ git clone https://github.com/vespa-cloud/cord-19-search.git &amp;&amp; cd cord-19-search
</pre>


Generate a `feed-file.jsonl` in current directory by following the procedure
in [feeding.md](feeding.md), or skip this step and use a small sample feed. 


Build the application using Maven:

<pre data-test="exec" data-test-expect="BUILD SUCCESS" data-test-timeout="600">
$ mvn -U clean install
</pre>


Start the vespa container
<pre data-test="exec">
$ docker run --detach --name cord19 --hostname vespa-container \
  --publish 8080:8080 --publish 19071:19071 \
  vespaengine/vespa
</pre>

Deploy the application. This step deploys the application package built in the previous step:

<pre data-test="exec" data-test-assert-contains="Success">
$ vespa deploy --wait 300
</pre>

Wait for the application endpoint to become available:
<pre data-test="exec">
$ vespa status --wait 300
</pre>

Feed sample data using the [Vespa CLI](https://docs.vespa.ai/en/vespa-cli.html#documents):
<pre data-test="exec">
$ zstdcat sample-feed/sample-feed.jsonl.zst | vespa feed -t http://localhost:8080 -
</pre>

Alternatively, feed the generated feed file `feed-file.jsonl`:
<pre>
$ vespa feed -t http://localhost:8080 feed-file.jsonl
</pre>

Run a query:
<pre data-test="exec" data-test-assert-contains='Prevention'>
$ vespa query 'yql=select title,abstract from doc where userQuery()' 'query=covid-19 prevention strategies' 'ranking=bm25'
</pre>

To print the `curl` equivelent use `vespa query -v`:
<pre>
$ vespa query -v 'yql=select title,abstract from doc where userQuery()' 'query=covid-19 prevention strategies' 'ranking=bm25'
</pre>

## Query api examples

ColBERT re-ranking only:

<pre data-test="exec" data-test-assert-contains='Prevention'>
$ vespa query 'yql=select title,abstract from doc where userQuery()' 'query=covid-19 prevention strategies' 'ranking=colbert'
</pre>

Hybrid re-ranking:
<pre data-test="exec" data-test-assert-contains='Prevention'>
$ vespa query 'yql=select title,abstract from doc where userQuery()' 'query=covid-19 prevention strategies' 'ranking=hybrid-colbert'
</pre>

Hybrid re-ranking and cross-encoder re-ranking:
<pre data-test="exec" data-test-assert-contains='Prevention'>
$ vespa query 'yql=select title,abstract from doc where userQuery()' 'query=covid-19 prevention strategies' 'ranking=hybrid-colbert' 'cross-rerank=true'
</pre>


Clean up and remove the container 
<pre data-test="after">
$ docker rm -f cord19
</pre>
