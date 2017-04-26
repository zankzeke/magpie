from __future__ import print_function

import numpy as np
import sys
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.preprocessing import Imputer

#
#  Iterative attribute selection using RandomForest
#
#  Works by iteratively selecting the attributes with the highest scores
#  (based on the attribute performance score provided by RandomForest),
#  and then repeating the process on the reduced set. This technique
#  can be used for both regression and classification models
#
#  Usage: python <this_file>.py <options...> < datafile.csv
#
#    Command Line Options
#
#  -num_attr <#> : Target number of attributes
#  -num_steps <#> : Number of steps in iterative selection process
#  -regression : Train a regression model
#  -num_trees <#> : Number of trees to use in RandomForest classifier
#
#    Procedure:
#
#  1. Determine a constant factor, f, such that f^n times the original 
#     number of attributes equals the target number of attributes, where
#     n is the number of loop steps
#  2. Train a RandomForest model on the data
#  3. Identify the top f% of attributes based on the 'feature importance' 
#     score from the RandomForest model
#  4. Eliminate all other attributes from the dataset
#  5. If the number of remaining attributes is greater than the target number,
#     repeat from Step 2
#
#  Author: Logan Ward <ward.logan.t@gmail.com>
#  Date: 21 September 2016
#

if __name__ == '__main__':
    # Default variables
    is_classifier = True
    num_attr = 3
    num_steps = 8
    num_trees = 100
    
    # Check user results
    pos = 1
    while pos < len(sys.argv):
        if sys.argv[pos] == '-num_attr':
            pos += 1
            num_attr = int(sys.argv[pos])
        elif sys.argv[pos] == '-num_steps':
            pos += 1
            num_steps = int(sys.argv[pos])
        elif sys.argv[pos] == '-num_trees':
            pos += 1
            num_trees = int(sys.argv[pos])
        elif sys.argv[pos] == '-regression':
            is_classifier = False
        else:
            print('[ERROR] Unrecognized command line argument: ', sys.argv[pos], file=sys.stderr)
        pos += 1

    # Get the mode used to perform attribute selection
    if is_classifier:
        model = RandomForestClassifier(n_estimators = num_trees)
    else:
        model = RandomForestRegressor(n_estimators = num_trees)

    # Get the data
    columns = sys.stdin.readline().split(",")
    if sys.version_info[0] == 2:
        data = np.genfromtxt(sys.stdin, delimiter=",")
    elif sys.version_info[0] == 3:
        data = np.genfromtxt(sys.stdin.buffer, delimiter=",")
    else:
        raise Exception('Unrecognized version of Python: %s' % str(sys.version_info))

    # Get rid of infinite values by marking them as missing
    data[np.isinf(data)] = np.nan
    
    # Impute the missing values on the dataset
    imputer = Imputer()
    imputer.fit(data)
    if any(np.isnan(imputer.statistics_)):
        print('[Status] Imputation failed for some attributes. Assigning them a constant value of 0')
        imputer.statistics_ = np.array([ 0 if np.isnan(x) else x for x in imputer.statistics_ ])
    data = imputer.transform(data)
    print(any(np.isnan(data.flatten())), any(np.isinf(data.flatten())))
        
    # Decide the steps used for the attribute selection
    steps = np.logspace(np.log10(num_attr), np.log10(len(columns)), num_steps, endpoint=False)
    steps = steps.astype(np.int)
    steps[0] = num_attr

    # Iteratively remove 50% of the attributes
    for target_attr in steps[::-1]:
        # Make a RF model
        X = data[:,:-1]
        y = data[:,-1]
        model.fit(X,y)

        # Rank attributes
        ranked_attrs = sorted(enumerate(model.feature_importances_), key=lambda x: -x[1])
        
        # Print out the current selections
        new_attr_ids = [x[0] for x in ranked_attrs[:target_attr]]
        new_attrs = [columns[c] for c in new_attr_ids]
        if len(new_attr_ids) < 15:
            print("[Status] Downselected to %d. Selected attributes: %s"%(target_attr, " ".join(new_attrs)))
        else:
            print("[Status] Downselected to %d. Top 15 selected attributes: %s"%(target_attr, " ".join(new_attrs[:15])))
        
        # Update the dataset and attribute list
        new_attr_ids.append(-1)
        columns = new_attrs
        data = data[:,new_attr_ids]
        
    print("[Answer]", " ".join(columns))
