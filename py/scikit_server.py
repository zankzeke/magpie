from __future__ import print_function
from __future__ import print_function

import numpy as np
import pickle as pickle
import socket
import sys
from sys import stdin, stdout, stderr

#
# Server used by Magpie to run machine learning models
#  written in Python. The model must follow the style
#  of scikit-learn models:
#    fit(X, y)  <- Train the model
#    predict(X) <- Evaluate model
#    predict_proba(X) <- Evaluate model, generate class probabilities
#
# This server is launched by feeding in a pickle object
#  describing the model via stdin. A server is then started
#  with a port number between 5482 and 5582, and the chosen
#  value is preinted to stdout.
#
# Once the server is launched, external software can communicate
#  with it over sockets. The first line of each message should
#  be a command word, followed by appropriate data for the command
#  Full details of the syntax are specified in the Magpie javadoc
#  for `magpie.models.utility.MultiModelUtility`
#
# This program also takes a few command line arguments
#   -classifier <- Check whether the model is a classifier (i.e.,
#                   that it supports: predict_proba)
#   -regressor <- Check whether the model is not a classifier
#
# Author: Logan Ward
# Date:   2 Feb 2017

print("Started", file=stderr)

# Useful variables to change
startPort = 5482  # First port to check
endPort = 5582  # Last port to check
verifyClassifier = None # Whether to crash if model is a classifier or not

# Check for command line arguments
if len(sys.argv) > 1:
    pos = 1
    while pos < len(sys.argv):
        action = sys.argv[pos].lower()
        if action == '-classifier':
            verifyClassifier = True
        elif action == '-regressor':
            verifyClassifier = False
        else:
            raise Exception("Command not recognized: " + action)
        pos += 1

# Load in model from standard in
if sys.version_info[0] == 2:
    model = pickle.load(stdin)
elif sys.version_info[0] == 3:
    model = pickle.loads(stdin.buffer.read())
else:
    raise Exception('Unrecognized version of Python: %s' % str(sys.version_info))

# If desired, verify that it is a classifier
if verifyClassifier is not None and hasattr(model, 'predict_proba') != verifyClassifier:
    raise Exception("Supplied model is not a %s!"%('classifier' if verifyClassifier else 'regressor'))

# Find a socket
port = startPort
ss = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
while port <= endPort:
    try:
        ss.bind(('localhost', port))
    except:
        port = port + 1
        continue
    break
ss.listen(0) # Only allow one connection
print("[Status] Listening on port:", port)
stdout.flush()

def trainModel(fi, fo):
    '''
    Train model after reading data from a client.
    
    Expects the number of rows followed by that many lines of input
    
    First number on line should be class, followed by attributes
    
    Receives rows of a matrix from a client
    Sends back the pickled model after training
    :param fi: File pointer to reading from socket
    :param client: Client for this server
    '''
    
    # Get the number of rows
    nRows = int(fi.readline())
    print("[Status] Recieving %d training entries"%nRows)
    
    # Read in the data
    data = np.genfromtxt(fi, max_rows=nRows)
    X = data[:, :-1]
    y = data[:, -1]

    # Train model
    print("[Status] Training model")
    model.fit(X,y)
    
    # Send model to client as compressed model
    print("[Status] Sending model back to client")
    fo = client.makefile('wb')
    pickle.dump(model, fo)


def runModel(fi, client):
    '''
    Train model after reading data from a client.
    
    Expects the number of rows followed by that many lines of input
    
    Recieves rows of a matrix from a client
    Sends back the predicted class values
    @param fi File pointer to reading from socket
    @param client Client for this server
    '''

    fo = client.makefile('w')

    # Receive data
    nRows = int(fi.readline())
    print("[Status] Receiving %d entries to run"%nRows)
    stdout.flush()
    X = np.genfromtxt(fi, max_rows=nRows)

    # Compute
    print("[Status] Running model")
    if hasattr(model, 'predict_proba'):
        y = model.predict_proba(X)
    else:
        y = model.predict(X)
    
    # Send back
    print("[Status] Sending results back to client")
    if hasattr(model, 'predict_proba'):
        for yi in y:
            print(' '.join([ str(c) for c in yi]), file=fo)
    else:
        for yi in y:
            print(yi, file=fo)

while 1:
    (client, address) = ss.accept()

    fi = client.makefile('rb')
    command = str(fi.readline())
    stderr.flush()
    if "train" in command:
        trainModel(fi, client)
    elif "run" in command:
        runModel(fi, client)
    elif "type" in command:
        print(model, file=client.makefile('w'))
    elif "exit" in command:
        print("[Status] Stopping server")
        exit()
    fi.close()
    
    # Close the client
    client.close()
