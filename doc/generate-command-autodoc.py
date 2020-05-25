import sys
from classFinder import *
from classHTMLRenderer import *

# The purpose this this code is to generate documentation for the
#  text interface of Magpie. It will parse the Javadoc from Magpie
#  to identify all implemented classes. Then, it assembles a website
#  that tells users the name of each class, how to instantiate it,
#  and what commands it can run.
#
# To Do List:
#  - Be able to tell when a command was overloaded
#  - Only crash when a single file is ill-formated
#	= Debatable: Catastrophic failure means documentation would mean that file would be more likely to be fixed
#
# Authors:
#   Logan Ward (ward.logan.t@gmail.com)

if len(sys.argv) != 2:
	print("Write documentation of how to instantiate and use Magpie variables")
	print("Usage: %s <javadoc dir>"%(sys.argv[0]))
	sys.exit()
docDir=sys.argv[1]

# Look for all classes
lib=ClassLibrary()
lib.findClasses(docDir, "magpie")

# Define useful functions
def printClassSummary(fp, classes):
	"""
	Generate a quick summary of each class in a list
	
	:param fp Pointer to output file
	:param classes List of classes
	"""
	# Get info
	output = []
	for cls in classes:
		path=HTMLRenderer.pathToDocumentationFile(cls)
		newLine = "<b><a href=\"" + path + "\">" + cls.package + "." + cls.name + "</a></b>"
		if len(cls.usage) > 1:
			newLine += ": Usage: " + cls.usage
		output.append(newLine)
	output.sort()
	
	# Print it
	started = False
	for line in output:
		toPrint = ""
		if not started: started = True
		else: toPrint += "</br>"
		toPrint += line
		print(toPrint, file=fp)

## Print header
fp = open("variables.html", "w")
print("<html>", file=fp)
print("<head>", file=fp)
print("\t<title>Magpie Variable Types</title>", file=fp)
print("\t<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" media=\"screen\" />", file=fp)
print("</head>", file=fp)

## Print introduction
print("<body>", file=fp)
print("<div id=\"wrapper\">", file=fp)
print("<div class=\"footer\">", file=fp)
print("\t<center><a href=\"index.html\">Manual Home</a></center>", file=fp)
print("</div>", file=fp)

print("<center><h1>Variable Types</h1></center>", file=fp)
print("<p>Magpie comes equipped with many different kinds of datasets, models, crystal structure prediction algorithms, and other kinds of variables. This section includes all of the currently available variable types and links to pages that describe what operations they&nbsp;support. If you are not yet familiar with how to call these operations, please consult the <a href=\"text-interface.html\">documentation for the text&nbsp;interface</a>.</p>", file=fp)

## Print dataset classes
print("<h2>Datasets</h2>", file=fp)
print("<p>Each of these dataset objects can be used to represent different kinds of data, both in terms of", file=fp)
print(" how Magpie represents an entry internally and what kind of attributes it can generate.</p>", file=fp)

classes = lib.getCompleteSubclasses("Dataset")
printClassSummary(fp, classes)
for cls in classes: 
	HTMLRenderer.writeDocumentationFile(docDir, cls, lib)
	
## Print model classes
print("<h2>Models</h2>", file=fp)
print("<p>Magpie is equipped with the ability to generate many different kinds of models. This includes ", file=fp)
print("models for classifying data into known subsets or predicting the value of some property.</p>", file=fp)
classes = lib.getCompleteSubclasses("BaseModel")

print("<h3>Classification Models</h3>", file=fp)
print("<p>Classifiers are used decide which group an entry belongs out of a finite list of options.</p>", file=fp)
subClasses = [ x for x in classes if "classifi" in x.package ]
printClassSummary(fp, subClasses)
for cls in subClasses: 
	HTMLRenderer.writeDocumentationFile(docDir, cls, lib)

print("<h3>Regression Models</h3>", file=fp)
print("<p>Regression models are used to approximate unknown, continuous", file=fp)
print(" functions (think y = f(x) = a + b * x).</p>", file=fp)
subClasses = [ x for x in classes if "regression" in x.package ]
printClassSummary(fp, subClasses)
for cls in subClasses: 
	HTMLRenderer.writeDocumentationFile(docDir, cls, lib)


## Print statistics classes
print("<h2>Statistics Calculators</h2>", file=fp)
print("<p>Each of these objects can be used calculate different statistics about the performance of a model.</p>", file=fp)

classes = lib.getCompleteSubclasses("BaseStatistics")
printClassSummary(fp, classes)
for cls in classes: 
	HTMLRenderer.writeDocumentationFile(docDir, cls, lib)

## Print clusterer classes
print("<h2>Clusterers</h2>", file=fp)
print("<p>Clustering algorithms perform unsupervised learning, which recognizes ", file=fp)
print("groups of data with similar attributes and provides rules for how to distinguish between them. ", file=fp)
print("These groups <i>are not</i> known beforehand, use classification algorithms to build rules for ", file=fp)
print("separating data into already-known groups.</p>", file=fp)

classes = lib.getCompleteSubclasses("BaseClusterer")
printClassSummary(fp, classes)
for cls in classes: 
	HTMLRenderer.writeDocumentationFile(docDir, cls, lib)

	
## Print Crystal Structure Predictors
print("<h2>Crystal Structure Predictors</h2>", file=fp)
print("<p>Crystal structure prediction algorithms are used to predict which crystal structure ", file=fp)
print("is most probable out of a list of known prototypes to be stable at a certain composition.</p>", file=fp)

classes = lib.getCompleteSubclasses("CSPEngine")
printClassSummary(fp, classes)
for cls in classes: 
	HTMLRenderer.writeDocumentationFile(docDir, cls, lib)

## Close up shop
print("</div>\n</body>", file=fp)
print("</html>", file=fp)
fp.close()

