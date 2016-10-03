from sklearn.ensemble import GradientBoostingClassifier, RandomForestRegressor
from sklearn.linear_model import LinearRegression
import pickle

# Write the GBC
gbc = GradientBoostingClassifier(n_estimators=10,
        learning_rate=0.001,
        max_depth=3,
        subsample=0.5)
pickle.dump(gbc, open('sklearn-gbc.pkl', 'wb'))

# Write the RF
rf = RandomForestRegressor()
pickle.dump(rf, open('sklearn-randomforest.pkl', 'wb'))

# Write the linear regression
lr = LinearRegression()
pickle.dump(lr, open('sklearn-linreg.pkl', 'wb'))
