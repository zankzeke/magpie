from __future__ import print_function
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

import pickle as pickle
from sys import argv, stdin, stdout, stderr
import socket
import array
import sys

# Useful variables to change
startPort = 5482; # First port to check
endPort = 5582; # Last port to check
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
model = pickle.load(stdin)

# If desired, verify that it is a classifier
if verifyClassifier is not None and hasattr(model, 'predict_proba') != verifyClassifier:
    raise Exception("Supplied model is not a %s!"%('classifier' if verifyClassifier else 'regressor'))

# Find a socket
port = startPort;
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
stdout.flush();

def trainModel(fi, fo):
    '''
    Train model after reading data from a client.
    
    Expects the number of rows followed by that many lines of input
    
    First number on line should be class, followed by attributes
    
    Receives rows of a matrix from a client
    Sends back the pickled model after training
    :param fi: File pointer to reading from socket
    :param fo: File pointer to writing to socket
    '''
    
    # Get the number of rows
    nRows = int(fi.readline())
    print("[Status] Recieving %d training entries"%nRows)
    
    # Read in the data
    X = []
    y = []
    for i in range(nRows):
        line = fi.readline()
        x = array.array('d')
        temp = [ float(w) for w in line.split() ]
        x.fromlist(temp[1:])
        X.append(x)
        y.append(temp[0])
    
    # Train model
    print("[Status] Training model")
    model.fit(X,y)
    
    # Send model to client as compressed model
    print("[Status] Sending model back to client")
    pickle.dump(model, fo)

def runModel(fi, fo):
    '''
    Train model after reading data from a client.
    
    Expects the number of rows followed by that many lines of input
    
    Recieves rows of a matrix from a client
    Sends back the predicted class values
    @param fi File pointer to reading from socket
    @param fo File pointer to writing to socket
    '''
    
    # Receive data
    nRows = int(fi.readline())
    print("[Status] Receiving %d entries to run"%nRows)
    X = []
    y = []
    for i in range(nRows):
        line = fi.readline()
        x = array.array('d')
        temp = [ float(w) for w in line.split() ]
        x.fromlist(temp)
        X.append(x)
    
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

    fi = client.makefile('r')
    fo = client.makefile('w')
    command = fi.readline()
    if "train" in command:
        trainModel(fi, fo)
    elif "run" in command:
        runModel(fi, fo)
    elif "type" in command:
        print(model, file=fo)
    elif "exit" in command:
        print("[Status] Stopping server")
        exit()
    fi.close()
    fo.close()
    
    # Close the client
    client.close()
