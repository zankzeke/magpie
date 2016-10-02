import cPickle as pickle
from sys import argv, stdin, stdout, stderr
import socket
import array

startPort = 5482; # First port to check
endPort = 5582; # Last port to check

# Load in model from standard in
model = pickle.load(stdin)

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
print "[Status] Listening on port:", port
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
    print "[Status] Recieving %d training entries"%nRows
    
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
    print "[Status] Training model"
    model.fit(X,y)
    
    # Send model to client as compressed model
    print "[Status] Sending model back to client"
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
    print "[Status] Receiving %d entries to run"%nRows
    X = []
    y = []
    for i in range(nRows):
        line = fi.readline()
        x = array.array('d')
        temp = [ float(w) for w in line.split() ]
        x.fromlist(temp)
        X.append(x)
    
    # Compute
    print "[Status] Running model"
    if hasattr(model, 'predict_proba'):
        y = model.predict_proba(X)
    else:
        y = model.predict(X)
    
    # Send back
    print "[Status] Sending results back to client"
    if hasattr(model, 'predict_proba'):
        for yi in y:
            print >>fo, ' '.join([ str(c) for c in yi])
    else:
        for yi in y:
            print >>fo, yi

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
        print >>fo, model
    elif "exit" in command:
        print "[Status] Stopping server"
        exit()
    fi.close()
    fo.close()
    
    # Close the client
    client.close()
