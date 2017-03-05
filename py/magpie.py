"""Simple API interface for Magpie server

Allows users to run simle commands on a Magpie server,
such as running models. This program takes a few different
commands as arguments

    version 
        Gets the API version of the Magpie server
    models <properties...>
        Gets the names of models available on this server,
        and prints out a few requested bits of information.
        See /api/swagger.yml:/definitions/ModelInformation
    run <models...>
        Run a list of models. Entries to be run should be passed to standard
        in with each entry on a single line.
        
        If format is "human", prints results to in a human-readable format
        If format is "csv", prints results as a single CSV file
    attributes <models...>
        Generates the attributes that serve as input to each model
        
        If multiple models are specified, this program returns the union
        of the attributes from all models.
        
        The first column will always be the entry name
        
By default, this program connects to a server hosted at Northwestern.
But, you can override this by adding a "--url <url>" option

Example commands
____________
    - Print out the property and unit of all models running on a specific server
        python magpie.py --url http://josquin.northwestern.edu:4581/ models property units
    - Run the "gfa" and "oqmd-dH" models for NaCl and print output to screen in a human-friendly format
        echo "NaCl" | python magpie.py run gfa oqmd-dH
    - Run the "gfa" and "oqmd-dH" models for NaCl and save detailed output into results.csv
        echo "NaCl" | python magpie.py --format csv --output results.csv run gfa oqmd-dH
    - Generate attributes used as input into the gfa model, save as results.csv
        echo "NaCl" | python magpie.py --format csv --output results.csv attributes gfa
Author: Logan Ward
Date: 4 March 2017
"""

import pandas as pd
import sys
import magpie
import argparse

actions=['version', 'models', 'run', 'attributes']

# Make the parser
parser = argparse.ArgumentParser(description='Commandline interface for Magpie REST API')
parser.add_argument('--url', dest='url', help='URL for Magpie server',
    default='http://josquin.northwestern.edu:4581/')
parser.add_argument('--output', help='output file', default=sys.stdout, type=argparse.FileType('w'))
parser.add_argument('action', help='desired action', choices=actions)
parser.add_argument('options', help='options for action', nargs=argparse.REMAINDER)
parser.add_argument('--format', help='format for model prediction/attribute output', choices=['csv','human'], default='human')

# Parse the options
args = parser.parse_args()

# Define where to pipe the output
out = args.output

# Connect to the server
m = magpie.MagpieServer(args.url)

# Run the appropriate action
if args.action == 'version':
    # Print out the API version
    print "Magpie API Version:", m.api_version()
elif args.action == 'models':
    model_info = m.models()
    
    # Print out the model information
    for model,info in model_info.iteritems():
        print >>out, "Model:", model
        for opt in args.options:
            print >>out, "\t%s:"%opt, info.get(opt, 'No such option')
elif args.action == 'run':
    # Get models to run
    models = args.options
    
    # Get the entries
    entries = [l.strip() for l in sys.stdin if len(l.strip()) > 0]
    
    # Collect the results
    results = dict([(model,m.run_model(model,entries)) for model in models])
    
    # Prepare to put all data in a single data frame
    output = pd.DataFrame(entries, columns=['Entry'])
    
    # Print results
    if args.format == 'human':
        # Get model information
        model_info = dict([(model,m.get_model_info(model)) for model in models])
    
        # Add in the results
        for model,result in results.iteritems():
            # Make a column header
            label = '%s:'%model if len(models) > 1 else ''
            label += model_info[model]['property']
            label += ' (%s)'%model_info[model]['units'] if model_info[model]['modelType'] == 'regession' else ''
            
            # Fill in the contents
            if model_info[model]['modelType'] == 'regression':
                output[label] = result.iloc[:,1]
            else:
                output[label] = result.apply(lambda x: '%s (%.2f%%)'%(x[2], x[3+x[1]]*100), axis=1)
        
        output.to_string(out, index=False)
    elif args.format == 'csv':
        # Add in the results
        for model,result in results.iteritems():
            if len(models) > 1:
                # Add model name to names
                result.columns = ['%s:%s'%(model,c) for c in result.columns]
            output = output.join(result.iloc[:,1:])
        output.to_csv(out, index=False)
elif args.action == 'attributes':
    # Get models to run
    models = args.options
    
    # Get the entries
    entries = [l.strip() for l in sys.stdin if len(l.strip()) > 0]
    
    # Collect the results
    results = dict([(model,m.generate_attributes(model,entries)) for model in models])
    
    # Prepare to put all data in a single data frame
    output = pd.DataFrame(entries, columns=['Entry'])
    
    # Combine the attributes
    for model,result in results.iteritems():
        # Add new attributes to the output dataframe
        curcols = set(output.columns)
        colstokeep = filter(lambda x: not x in curcols, result.columns[1:])
        output = output.join(result[colstokeep])
    
    if args.format == 'human':
        output.to_string(out, index=False)
    elif args.format == 'csv':
        output.to_csv(out, index=False)
else:
    raise Exception('Action not implemented yet: ' + args.action)