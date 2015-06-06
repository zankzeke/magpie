import pickle
from sys import argv
import socket
import array

# Default values
startPort = 5482; # First port to check
endPort = 5582; # Last port to check

<<<<<<< HEAD
# Open the model file
fp = open(argv[1], 'r')
model = pickle.load(fp)
fp.close()

# Launch the server
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
print "Listening on port", port

# Functions to use model
def trainModel(fi, fo):
	'''
	Train model after reading data from a client.
	
	Expects the number of rows followed by that many lines of input
	
	First number on line should be class, followed by attributes
	
	Recieves rows of a matrix from a client
	Sends back the pickled model after training
	@param fi File pointer to reading from socket
	@param fo File pointer to writing to socket
	'''
	
	# Recieve
	print "Training model"
	nRows = int(fi.readline())
	print "\tExpected %d rows"%nRows
	X = []
	y = []
	for i in range(nRows):
		line = fi.readline()
		x = array.array('d')
		temp = [ float(w) for w in line.split() ]
		x.fromlist(temp[1:])
		X.append(x)
		y.append(temp[0])
	print "\tRead %d rows"%len(X)
	model.fit(X,y)
	
	# Send back
	print "\tSending back model"
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
	
	# Recieve
	print "Running model"
	nRows = int(fi.readline())
	print "\tExpected %d rows"%nRows
	X = []
	y = []
	for i in range(nRows):
		line = fi.readline()
		x = array.array('d')
		temp = [ float(w) for w in line.split() ]
		x.fromlist(temp)
		X.append(x)
	print "\tRead %d rows"%len(X)
	
	# Compute
	print "\tRunning model"
	y = model.predict(X)
	
	# Send back
	print "\tSending results"
	for yi in y:
		print >>fo, yi

# Listen for clients	
while 1:
	(client, address) = ss.accept()
	
	# Got a client, ask what it wants
	fi = client.makefile('r')
	fo = client.makefile('w')
	command = fi.readline()
	print "Recieved command", command
	if "train" in command:
            trainModel(fi, fo)
	elif "run" in command:
            runModel(fi, fo)
        elif "type" in command:
            print >>fo, model
=======
# Launch the server
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
print "Listening on port", port

# Open the model file
fp = open(argv[1], 'r')
model = pickle.load(fp)
fp.close()

# Functions to use model
def trainModel(fi, fo):
	'''
	Train model after reading data from a client.
	
	Expects the number of rows followed by that many lines of input
	
	First number on line should be class, followed by attributes
	
	Recieves rows of a matrix from a client
	Sends back the pickled model after training
	@param fi File pointer to reading from socket
	@param fo File pointer to writing to socket
	'''
	
	# Recieve
	print "Training model"
	nRows = int(fi.readline())
	print "\tExpected %d rows"%nRows
	X = []
	y = []
	for i in range(nRows):
		line = fi.readline()
		x = array.array('d')
		temp = [ float(w) for w in line.split() ]
		x.fromlist(temp[1:])
		X.append(x)
		y.append(temp[0])
	print "\tRead %d rows"%len(X)
	model.fit(X,y)
	
	# Send back
	print "\tSending back model"
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
	
	# Recieve
	print "Running model"
	nRows = int(fi.readline())
	print "\tExpected %d rows"%nRows
	X = []
	y = []
	for i in range(nRows):
		line = fi.readline()
		x = array.array('d')
		temp = [ float(w) for w in line.split() ]
		x.fromlist(temp)
		X.append(x)
	print "\tRead %d rows"%len(X)
	
	# Compute
	print "\tRunning model"
	y = model.predict(X)
	
	# Send back
	print "\tSending results"
	for yi in y:
		print >>fo, yi
	
def trainModel(fi, fo):
	'''
	Train model after reading data from a client.
	
	Expects the number of rows followed by that many lines of input
	
	Recieves rows of a matrix from a client
	Sends back the pickled model after training
	@param fi File pointer to reading from socket
	@param fo File pointer to writing to socket
	'''
	
	# Recieve
	print "Training model"
	nRows = int(fi.readline())
	print "\tExpected %d rows"%nRows
	X = []
	y = []
	for i in range(nRows):
		line = fi.readline()
		x = array.array('d')
		temp = [ float(w) for w in line.split() ]
		x.fromlist(temp[1:])
		X.append(x)
		y.append(temp[0])
	print "\tRead %d rows"%len(X)
	model.fit(X,y)
	
	# Send back
	print "\tSending back model"
	pickle.dump(model, fo)

# Listen for clients	
while 1:
	(client, address) = ss.accept()
	
	# Got a client, ask what it wants
	fi = client.makefile('r')
	fo = client.makefile('w')
	command = fi.readline()
	print "Recieved command", command
	if "train" in command:
		trainModel(fi, fo)
	elif "run" in command:
		runModel(fi, fo)
>>>>>>> origin/master
	fi.close()
	fo.close()
		
	# Close the client
	client.close()

