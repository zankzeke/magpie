import socket
from sys import argv
import pickle
from numpy import mean

port = int(argv[1])

# Open up the port
def connect(port):
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.connect(('localhost', port))
	fo = s.makefile('w')
	fi = s.makefile('r')
	return s, fo, fi
	
# Connect to server
s, fo, fi = connect(port)

# Make fake dataset
X = [[x, 1/(x+1), x+4, x*x] for x in range(10)]
y = [x[0] * 2 + x[0] * x[0] for x in X]

# Send in training data
print >>fo, "train"
print >>fo, len(y)
fo.flush()
for xi,yi in zip(X,y):
	print >>fo, yi, " ".join([str(xij) for xij in xi])
fo.flush()

# Get the model back
model = pickle.load(fi)
print model

# Close up
fo.close()
fi.close()
s.close()

# Make another call to run the model
s, fo, fi = connect(port)
print >>fo, "run"
print >>fo, len(X)
for xi in X:
	print >>fo, " ".join(str(xij) for xij in xi)
fo.flush()
y_pred_socket = [ float(fi.readline()) for i in range(len(X)) ]
y_pred_here = model.predict(X)

# Close up
fo.close()
fi.close()
s.close()

# Check difference
error = mean([ abs(i-j) for i,j in zip(y_pred_socket, y_pred_here) ])
print "Difference between here & server:", error
