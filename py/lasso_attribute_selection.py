from __future__ import print_function

import itertools
import numpy as np
import sys
from scipy.optimize import brentq
from sklearn.model_selection import cross_val_score, ShuffleSplit
from sklearn.externals.joblib import Parallel, delayed
from sklearn.linear_model import Lasso, LinearRegression
from sklearn.linear_model.coordinate_descent import _alpha_grid
from sklearn.metrics import mean_squared_error
from sklearn.preprocessing import MinMaxScaler

#
# Implements the LASSO-based attribute selection procedure
#  demonstrated by Ghiringhelli et al. (see PRL 114, 105503 (2015))
#  and later extended by Pilania et al. (see Sci. Rprts. 6, 19375 (2016))
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
#           removing the strongly-correlated attributes (default = skip this step)
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

# Utility functions
def run_score(comb, score, **kwargs):
    return (comb, score(comb, **kwargs))
def score_train(comb, model, X, y, cv):
    model = model()
    X_sub = X[:,comb]
    model.fit(X_sub,y)
    y_pred = model.predict(X_sub)
    return mean_squared_error(y, y_pred)
def score_cv(comb, model, X, y, cv):
    return -1 * np.mean(cross_val_score(model(), X[:,comb], y, scoring='neg_mean_squared_error', \
                                        cv=ShuffleSplit(n_splits=cv[1], test_size=cv[0], random_state=1)))

def run_lasso_selector():
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
        print("[Warning] You're asking this code to find the best attribute set size and not using cross-validation", file=sys.stderr)
        print("[Warning]     Adding attributes will only make the fitness to trainng set get better for linear regression", file=sys.stderr)
        print("[Warning]     Using CV by adding a \"-cv_method\" flag is *highly* recommended", file=sys.stderr)

    # Get the data
    columns = sys.stdin.readline().split(",")
    if sys.version_info[0] == 2:
        data = np.genfromtxt(sys.stdin, delimiter=",", dtype=np.float32)
    elif sys.version_info[0] == 3:
        data = np.genfromtxt(sys.stdin.buffer, delimiter=",", dtype=np.float32)
    else:
        raise Exception('Unrecognized version of Python: %s' % str(sys.version_info))
    X = data[:,:-1]
    y = data[:,-1]
    del data # No longer needed in memory
    print("[Status] Read in %d entries with %d attributes"%(X.shape))
    sys.stdin.flush()
    
    # Convert X to fortran format
    X = np.array(X, order='F')

    # Scale the features
    scaler = MinMaxScaler()
    X = scaler.fit_transform(X, y)
    print("[Status] Scaled entries to exist on same range")
    sys.stdin.flush()

    # Sanity check: Make sure n_params_lasso is less
    #  than the total number of attributes
    n_params_lasso = min(n_params_lasso, len(columns) - 1)

    # Compute the first n_params_lasso from the lasso path
    alphas = _alpha_grid(X, y)
    alpha_guess = alphas[0]
    
    def get_count(alpha):
        model = Lasso(alpha=alpha)
        for max_iter in [1e3,3e3,1e4,3e4,1e5]:
            model.fit(X,y)
            if model.n_iter_ < max_iter:
                break
        return np.count_nonzero(model.coef_), model.coef_
    
    #   Find the left end
    min_alpha = alpha_guess
    while get_count(min_alpha)[0] < n_params_lasso:
        min_alpha /= 10
    
    #   Find right end
    max_alpha = min_alpha
    while get_count(max_alpha)[0] > n_params_lasso:
        max_alpha *= 10
        
    res = brentq(lambda x: get_count(x)[0] - n_params_lasso, min_alpha, max_alpha)
    count, coef = get_count(res)

    # Get the LASSO selected attributes
    attr_ids = list(np.nonzero(coef)[0])
    print("[Status] Selected %d attributes via LASSO: "%len(attr_ids), " ".join([ columns[x] for x in attr_ids ]))
    sys.stdin.flush()

    # Optional: Iteratively remove highly-correlated attributes
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
        print("[Status] Downselected to %d loosely-correlated attributes: "%len(attr_ids), " ".join([ columns[x] for x in attr_ids ]))
        sys.stdin.flush()

    # Downsize the data arrays
    X = X[:, attr_ids]
    columns = [ columns[x] for x in attr_ids ]
    attr_ids = list(range(len(attr_ids)))

    # Loop through all possible combinations
    best_score_of_all = float('inf')
    best_comb_of_all = None
    dim_range = list(range(1,max_dimensionality+1)) if get_best else [max_dimensionality]
    score_func = score_cv if cv is not None else score_train
    for dim in dim_range:
        scores = Parallel(n_jobs=n_procs)([ \
            delayed(run_score)(comb, score_func, model=final_model, X=X, y=y, cv=cv) \
            for comb in itertools.combinations(attr_ids, dim) \
        ])
        best_comb, best_score = min(scores, key=lambda x: x[1])
        if best_score < best_score_of_all:
            best_comb_of_all = best_comb
            best_score_of_all = best_score
    
        print('[Status]', best_score, " ".join([columns[c] for c in best_comb]))
        sys.stdin.flush()

    # If user wanted the best choice
    print('[Answer]', " ".join([columns[c] for c in best_comb_of_all]))
    sys.stdin.flush()

if __name__ == '__main__':
    run_lasso_selector()