# Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#!/bin/bash

DIR="$1"
echo "[INFO] Downloading models into $DIR"

mkdir -p $DIR

echo "Downloading https://data.vespa.oath.cloud/onnx_models/trec_covid_synthetic.onnx"
curl -L -o $DIR/trec_covid_synthetic.onnx \
https://data.vespa.oath.cloud/onnx_models/trec_covid_synthetic.onnx

echo "Downloading https://data.vespa.oath.cloud/onnx_models/vespa-colMiniLM-L-6-dynamic-quantized.onnx"
curl -L -o $DIR/colbert_encoder.onnx \
https://data.vespa.oath.cloud/onnx_models/vespa-colMiniLM-L-6-dynamic-quantized.onnx
