from __future__ import print_function
import unittest
import magpie
from pprint import pprint

class MagpieTest(unittest.TestCase):

    server = None

    def setUp(self):
        self.server = magpie.MagpieServer()

    def test_connection(self):
        self.assertEquals(magpie._api_version, self.server.api_version())
        
    def test_status(self):
        status = self.server.status()
        self.assertEquals(magpie._api_version, status["apiVersion"])
        
    def test_models(self):
        models = self.server.models()
        self.assertTrue(isinstance(models, dict))
        
    def test_model_info(self):
        # Get the list of all models
        models = self.server.models()
        
        # Make sure the test server has some models
        self.assertTrue(len(models) > 0)
        
        # Get a key
        model = list(models.keys())[0]
        
        # Get the model info
        self._models = None
        model_info = self.server.get_model_info(model)
        self.assertEquals(models[model], model_info)
        
    def test_attributes(self):
        # Get a key
        model = 'gfa'
        
        # Run the attributes
        attrs = self.server.generate_attributes(model, ['NaCl', 'Zr2Al3Ti'])
        self.assertEquals((2,145), attrs.shape)
        
    def test_run_classifier(self):
        # Get a key
        model = 'gfa'
        
        # Run the attributes
        results = self.server.run_model(model, ['NaCl', 'Zr2Al3Ti'])
        results.to_csv('data.csv', index=False)
        self.assertEquals((2,5), results.shape)
        
    def test_run_regression(self):
        # Get a key
        model = 'oqmd-dH'
        
        # Run the attributes
        results = self.server.run_model(model, ['NaCl', 'Zr2Al3Ti'])
        self.assertEquals((2,2), results.shape)
        
