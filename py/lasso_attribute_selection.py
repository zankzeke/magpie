from sklearn.linear_model import Lasso, lasso_path, LinearRegression
from sklearn.cross_validation import cross_val_score, ShuffleSplit
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_squared_error
from sklearn.externals.joblib import Parallel, delayed
import math
import sys
import pandas as pd
import itertools
import numpy as np

#
# Implements the LASSO-based attribute selection procedure
#  demonstrated by Ghiringhelli et al. (see PRL 114, 105503 (2015))
#  and later extended by Pilania et al. (see Sci. Rprts. 6, 19375 (2016)
#
# Method:
#
#  1. Use LASSO to determine a subset of attributes that create 
#     a sparse, linear model of a property
#  2. Optionally: Iteratively remove the attributes that have the 
#      highest pearson's correlation with all other attributes
#      a. This step was introduced by Pilania
#  3. Within the current set of attributes, find the subset
#     that creates the best linear model. There are a few ways
#     of approaching this, and our package implements two
#      a. Measure the fitness of the model on the entire dataset (Ghiringhelli)
#          *or* the performance of the model in cross-validatin (Pilania)
#      b. Select a fixed attribute set size *or* try many set
#          sizes and select the one that leads to best score
#
#
#  Usage: python <this_file>.py <options...> < datafile.csv
#
#  This code reads in data from stdin as a csv file
#
#  Options are provided as flags when calling the python code, 
#   and may be listed in any order:
#
#    -n_lasso <#> : Number of attributes to select using LASSO (default = 16)
#    -corr_downselect <#> : Number of attributes to downselect to by 
# 		    removing the strongly-correlated attributes (default = skip this step)
#    -max_dim <#> : Number of attributes to select from OLS regression step (default = 5)
#    -n_procs <#> : Number of processors on which to run cross-validation (default = 1)
#    -pick_best   : Whether to set subsets up to max_dim in size, and to select the one 
#                   with the highest score (default = No)
#    -cv_method <split_fraction> <# iter> : Set fraction of dataset to hold out as the 
#                   test set, and how many times to repeat CV
#
#
#  Author: Logan Ward <ward.logan.t@gmail.com>
#  Date: 14 April 2016
#


# Default parameters
n_params_lasso = 16
corr_downselect = None
max_dimensionality = 5
final_model = LinearRegression
n_procs = 1
get_best = False 
cv = None

# Loop over argv
pos = 1
while pos < len(sys.argv):
	cmd = sys.argv[pos].lower()
	if cmd == "-n_lasso":
		pos += 1
		n_params_lasso = int(sys.argv[pos])
	elif cmd == "-corr_downselect":
		pos += 1
		corr_downselect = int(sys.argv[pos])
	elif cmd == "-max_dim":
		pos += 1
		max_dimensionality = int(sys.argv[pos])
	elif cmd == "-n_procs":
		pos += 1
		n_procs = int(sys.argv[pos])
	elif cmd == "-pick_best":
		get_best = True
	elif cmd == "-cv_method":
		pos += 1
		split_frac = float(sys.argv[pos])
		if split_frac <= 0 or split_frac >= 1:
			raise Exception('Split fraction must be between 0 and 1')
		pos += 1
		niter = int(sys.argv[pos])
		cv = (split_frac, niter)
	else:
		raise Exception('command %s not recognized'%cmd)
	pos += 1

# Raise a few warnings
if cv is None and get_best:
	print >>sys.stderr, "[Warning] You're asking this code to find the best attribute set size and not using cross-validation"
	print >>sys.stderr, "[Warning]     Adding attributes will only make the fitness to trainng set get better for linear regression"
	print >>sys.stderr, "[Warning]     Using CV by adding a \"-cv_method\" flag is *highly* recommended"

# Get the data
data = pd.read_csv(sys.stdin)
X = data[data.columns[:-1]].as_matrix()
y = data[data.columns[-1]].as_matrix()
columns = data.columns
del data # No longer needed in memory

# Scale the features
scaler = MinMaxScaler()
X = scaler.fit_transform(X, y)

# Sanity check: Make sure n_params_lasso is less
#  than the total number of attributes
n_params_lasso = min(n_params_lasso, len(columns) - 1)

# Compute the first n_params_lasso from the lasso path
alphas, coefs, dual_path  = lasso_path(X, y)
for coef in coefs.T:
	count = np.count_nonzero(coef)
	if count >= n_params_lasso:
		break
if count < n_params_lasso:
	# Increase manually
	alpha = alphas[-1]
	alpha_step = alpha / alphas[-2]
	while count < n_params_lasso:
		alpha = alpha * alpha_step
		model = Lasso(alpha=alpha)
		model.fit(X,y)
		coef = model.coef_
		count = np.count_nonzero(coef)
		

# Get the LASSO selected attributes
attr_ids = list(np.nonzero(coef)[0])
print "[Status] Selected %d attributes via LASSO: "%len(attr_ids), " ".join([ columns[x] for x in attr_ids ])

# Optinal: Iteratively remove highly-correlated attributes
if not corr_downselect is None:
	while len(attr_ids) > corr_downselect:
		# Get the attributes
		X_sub = X[:,attr_ids]

		# Compute squared correlation between everyone
		corr = np.corrcoef(X_sub)
		corr = np.power(corr, 2)

		# Compute the average correlation for each attribute
		attr_corr = [ (attr_ids[i], np.mean(corr[:,i]) + np.mean(corr[i,:])) for i in range(len(attr_ids)) ]

		# Remove the highest
		highest_attr = max(attr_corr, key=lambda x: x[1])[0]
		attr_ids.remove(highest_attr)
	print "[Status] Downselected to %d loosely-correlated attributes: "%len(attr_ids), " ".join([ columns[x] for x in attr_ids ])


# Define method used to compute CV score
#  A function to return MSE given a reduced dataset
if cv is None:
	def score(X):
		model = final_model()
		model.fit(X,y)
		y_pred = model.predict(X)
		return mean_squared_error(y, y_pred)
else:
	def score(X):
		return -1 * np.mean(cross_val_score(final_model(), X, y, 'mean_squared_error', \
			cv=ShuffleSplit(len(y), n_iter=cv[1], test_size=cv[0], random_state=1)))

# Loop through all possible combinations
best_score_of_all = float('inf')
best_comb_of_all = None
dim_range = range(1,max_dimensionality+1) if get_best else [max_dimensionality]
for dim in dim_range:
	def run_score(comb):
		return (comb, score(X[:,comb]))
	scores = Parallel(n_jobs=n_procs)([ \
		delayed(run_score)(comb) \
		for comb in itertools.combinations(attr_ids, dim) \
	])
	best_comb, best_score = min(scores, key=lambda x: x[1])
	if best_score < best_score_of_all:
		best_comb_of_all = best_comb
		best_score_of_all = best_score
		
	print '[Status]', best_score, " ".join([columns[c] for c in best_comb])
	
# If user wanted the best choice
print '[Answer]', " ".join([columns[c] for c in best_comb_of_all])
