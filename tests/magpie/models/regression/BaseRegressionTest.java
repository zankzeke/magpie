/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;

import magpie.data.Dataset;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test regression model capabilities.
 * @author Logan Ward
 */
public class BaseRegressionTest {
	private Dataset data = new Dataset();
	
	public BaseRegressionTest() {
		try {
			data.clearData();
			data.importText("datasets/simple-data.csv", null);
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}

	@Test
	public void testSimpleTraining() {
		PolynomialRegression model = new PolynomialRegression();
		model.setOrder(2);
		model.train(data);
	}
	
	@Test
	public void testMixedTraining() {
		PolynomialRegression model = new PolynomialRegression();
		model.setOrder(2);
		
		Dataset clone = data.clone();
		clone.addEntry("0.0, -1.5");
		
		model.train(clone);
	}
	
	public void testCrossValidation() {
		PolynomialRegression model = new PolynomialRegression();
		model.setOrder(2);
		
		model.crossValidate(10, data);
	}
}
