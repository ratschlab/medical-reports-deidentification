import sys
import pandas as pd
import shutil
import os
import glob
import gc


json_dir = sys.argv[1] # '/home/marczim/data/deid_poc/sets/kisim/diagnostics/full/ml-features.json'

parquet_dir = json_dir + "_parquet"

if os.path.exists(parquet_dir):
    shutil.rmtree(parquet_dir)

os.mkdir(parquet_dir)


for path in glob.glob(os.path.join(json_dir, '*.json')):
    print(f"processing {path}")
    df = pd.read_json(path)

    df = df[['tokenText', 'posTag', 'label', 'manualLabel', 'lookupTags', 'docType', 'sentenceBeginning', 'fieldName', 'fieldPath', 'labelExtended', 'manualLabelExtended',
       'sentenceNr', 'tokenKind',
       'tokenNr', 'triggeredRule', 'docId']]


    df['isCapitalized'] = df['tokenText'].str.get(0).str.isupper()
    df['locationLookup'] = (df['lookupTags'].str.contains('location-city')) | (df['lookupTags'].str.contains('location-country'))
    df['nameLookup'] = (df['lookupTags'].str.contains('person_first')) | (df['lookupTags'].str.contains('surname'))
    df['generalLookup'] = (df['lookupTags'].str.contains('general'))
    df['medicalLookup'] = (df['lookupTags'].str.contains('medical'))
    df['locationLabel'] = (df['label'].str.contains('Location'))
    df['nameLabel'] = (df['label'].str.contains('Name'))
    df['dateLabel'] = (df['label'].str.contains('Date'))
    
    df.to_parquet(os.path.join(parquet_dir, os.path.basename(path).replace('.json', '.parquet')))
    
    del df
    gc.collect()


