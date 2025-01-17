#!/usr/bin/env python3
# Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

import pandas
import sys
import json
import datetime
from os import path
import numpy as np

def get(df_row, key, defaultValue):
  value = df_row[key] 
  if value == 'notvalid':
    return defaultValue
  else:
    return value

def fall_back_authors(authors):
  if not authors:
    return []
  json_authors = []
  #Taslim Ali, Sheikh; Kadi, A. S.; Ferguson, Neil M.
  for a in authors.split(';'):
    parts = a.split(',')
    firstname = None
    lastname = None
    if len(parts) < 2:  
      lastname = parts[0]
    else:
      lastname,firstname = parts[0],parts[1]
    name = lastname
    if lastname and firstname:
      name = '%s %s' % (firstname, lastname)
    author = {
      'first': firstname,
      'last': lastname,
      'name': name 
    }
    json_authors.append(author)
  return json_authors 

def produce_vespa_json(idx, row):
  title = get(row,'title',None)
  abstract = get(row,'abstract',None)
  sha = row['sha']
  source = row['source_x']
  license = get(row, 'license', None)
  journal = get(row, 'journal', None)
  url = get(row, 'url', None)
  cord_uid = get(row, 'cord_uid', None)
  pmcid = get(row, 'pmcid',None)
  pubmed_id  = get(row, 'pubmed_id',None)
  if pubmed_id != None:
    try:
      pubmed_id = int(pubmed_id)
    except:
      pubmed_id = None 
  publish_time = get(row, 'publish_time', None)
  timestamp = 0
  try:
    timestamp = int(datetime.datetime.strptime(publish_time, '%Y-%m-%d').timestamp())
  except:
    pass
  doi = get(row, 'doi', None)
  authors = fall_back_authors(get(row, 'authors',None))

  if doi:
    doi = 'https://doi.org/%s' % doi 

  vespa_doc = {
    'title': title,
    'id': idx, 
    'source': source,
    'license': license,
    'datestring': publish_time,
    'doi': doi,  
    'url': url, 
    'cord_uid': cord_uid, 
    'authors': authors,
    'abstract': abstract,
    'journal': journal,
    'timestamp': timestamp,
    'pmcid': pmcid,
    'pubmed_id': pubmed_id
  } 
  return vespa_doc

META_FILE = sys.argv[1]

df = pandas.read_csv(META_FILE)
df = df.fillna("notvalid")

docs = []
for idx, row in df.iterrows():
  doc = produce_vespa_json(idx,row)
  id = doc['id']
  doc = {
    "put": "id:covid-19:doc::%i" % id,
    "fields": doc
  }
  print(json.dumps(doc))
