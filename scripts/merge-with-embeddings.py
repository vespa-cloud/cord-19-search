#!/usr/bin/env python3
# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
import sys
import numpy 
import json

docs = dict()
with open("feed.jsonl") as fp:
	for line in fp:
		doc = json.loads(line)
		id = doc['fields']['id']
		cord_id = doc['fields']['cord_uid']
		docs[cord_id] = doc

for line in sys.stdin:
	line = line.strip()
	tap = line.find(',')
	cord_id = line[0:tap]
	vector = line[tap+1:]
	v = eval("[" + vector + "]")
	doc = docs[cord_id]
	doc['fields']['specter_embedding'] = {
		"values": v
	}
	print(json.dumps(doc))
