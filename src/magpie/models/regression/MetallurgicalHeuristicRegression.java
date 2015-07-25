
package magpie.models.regression;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Estimate formation energy for ternary+ alloys using a metallurgical heuristic. 
 * Works by estimating multi-component formation energy based on formation energies of binaries.
 * See <a href="http://www.cambridge.org.turing.library.northwestern.edu/us/academic/subjects/engineering/materials-science/phase-equilibria-phase-diagrams-and-phase-transformations-their-thermodynamic-basis-2nd-edition">
 * Hillert's</a> text for a detailed description.
 * 
 * <p><usage><b>Usage</b>: $&lt;hull data>
 * <br><pr><i>hull data</i>: A {@linkplain CompositionDataset} containing formation energies for all known binary compounds.</usage>
 * 
 * @author Logan Ward
 */
public class MetallurgicalHeuristicRegression extends BaseRegression {
	/** Holds binary convex hulls */
	private BinaryConvexHullHolder HullHolder;

	@Override
	public void setOptions(List<Object> Options) throws Exception {
		try {
			setBinaryConvexHulls((CompositionDataset) Options.get(0));
		} catch (Exception e) {
			throw new Exception(printUsage());
		}
	}

	@Override
	public String printUsage() {
		return "Usage: $<convex hull data>";
	}
	
	/**
	 * Define binary convex hull data. Should contain compositions and formation energies
	 * (&quot;delta_e&quot;) for each known binary and elemental compounds that 
	 * <b>are on the convex hull</b>. You also should output the formation energy
	 * for each element (which may be non-zero if your dataset uses fitted reference states).
	 * 
	 * <p>This code does not currently have the capability to calculate convex hulls. Since, this
	 * class is intended to be used with data from <a href="https://github.com/wolverton-research-group/qmpy">qmpy</a> I 
	 * figured that you can just use that code's capabilities.
	 * 
	 * @param hullData Dataset holding all known compounds to be used for calculating delta_e
	 */
	public void setBinaryConvexHulls(CompositionDataset hullData) {
		HullHolder = new BinaryConvexHullHolder(hullData);
	}	

	@Override
	protected void train_protected(Dataset TrainData) {
		// Nothing to train
		if (! (TrainData instanceof CompositionDataset)) {
			throw new Error("Data must extend CompositionDataaset");
		}
	}

	@Override
	public void run_protected(Dataset TrainData) {
		if (! (TrainData instanceof CompositionDataset)) {
			throw new Error("Data must extend CompositionDataaset");
		}
		double[] predictions = new double[TrainData.NEntries()];
		CompositionDataset Ptr = (CompositionDataset) TrainData;
		for (int i=0; i<TrainData.NEntries(); i++) {
			predictions[i] = HullHolder.evaluateCompound(Ptr.getEntry(i));
		}
		TrainData.setPredictedClasses(predictions);
	}

	@Override
	protected String printModel_protected() {
		return "Metallurgical heuristic model. Using " + HullHolder.NBinaries 
                + " binary compounds.";
	}

    @Override
    protected List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat); 
        output.add("Binary hulls based on " + HullHolder.NBinaries + " compounds");
        return output;
    }
    
    
    
	@Override
	public int getNFittingParameters() {
		return 0;
	}
}

/**
 * Contains operations to extract and retrieve binary convex hulls. 
 * 
 * @author Logan Ward 
 */
class BinaryConvexHullHolder {
	/** 
	 * Processed convex hulls. Key values are the two elements (Left &lt; Right).
	 * Values are always (x<sub>left</sub>, &Delta;H<sub>f</sub>), sorted by x.
	 */
	final Map<ImmutablePair<Integer,Integer>, 
			List<ImmutablePair<Double,Double>>> ConvexHulls = new TreeMap<>();
    /** Number of binary compounds stored in this object */
    int NBinaries;
	
	public BinaryConvexHullHolder(CompositionDataset HullData) {
        NBinaries = 0;
		/** --> First, assemble map */ 
		int NElem = HullData.ElementNames.length;
		for (int i = 0; i < NElem; i++) {
			for (int j = i + 1; j < NElem; j++) {
				ConvexHulls.put(new ImmutablePair<>(i, j), new LinkedList<ImmutablePair<Double, Double>>());
			}
		}
		
		/** Make sure we have a delta_e property */
		int deltaEindex = HullData.getPropertyIndex("delta_e");
		if (deltaEindex == -1) {
			throw new Error("ERROR: Dataset must contain delta_e as a property");
		}
		
		/** Now, add all points from dataset */
		for (BaseEntry Entry : HullData.getEntries()) {
			CompositionEntry Ptr = (CompositionEntry) Entry;
			int[] elem = Ptr.getElements();
			// If entry has 2 elements, store the composition on the appropriate map
			if (elem.length == 2) {
                NBinaries++;
				double[] frac = Ptr.getFractions();
				// Get the lookup key
				ImmutablePair<Integer,Integer> key;
				key = elem[0] < elem[1] ? new ImmutablePair<>(elem[0],elem[1])
						: new ImmutablePair<>(elem[1], elem[0]);
				
				// Prepare point to add to chart
				ImmutablePair<Double,Double> point;
				double delta_e = Ptr.getMeasuredProperty(deltaEindex);
				point = elem[0] < elem[1] ? new ImmutablePair<>(frac[0],delta_e)
						: new ImmutablePair<>(frac[1], delta_e);
				
				// Add point to desired binary
				List<ImmutablePair<Double,Double>> binary = ConvexHulls.get(key);
				if (binary == null) {
					throw new Error("ERROR: Binary not found.");
				} 
				binary.add(point);
				
			} else if (elem.length == 1) {
				// If only one elements, store it on every map contining that element
				double delta_e = Ptr.getMeasuredProperty(deltaEindex);
				for (ImmutablePair<Integer,Integer> key : ConvexHulls.keySet()) {
					if (key.getLeft() == elem[0]) {
						ImmutablePair<Double,Double> point = new ImmutablePair<>(1.0,delta_e);
						ConvexHulls.get(key).add(point);
					} else if (key.getRight() == elem[0]) {
						ImmutablePair<Double,Double> point = new ImmutablePair<>(0.0,delta_e);
						ConvexHulls.get(key).add(point);
					}
				}
			}
		}	
		
		/** Sort each binary convex hull */
		for (List<ImmutablePair<Double,Double>> binary : ConvexHulls.values()) {
			Collections.sort(binary);
		}
	}
		
	/**
	 * Evaluate the binary convex hull energy at a certain point.
	 * 
	 * @param elemA Element #1
	 * @param elemB Element #2
	 * @param fracA Fraction of element #1
	 * @return Formation energy of convex hull at that point
	 */
	public double evaluatePoint(int elemA, int elemB, double fracA) {
		// Determine lookup information
		ImmutablePair<Integer,Integer> binary;
		binary = elemA < elemB ? new ImmutablePair<>(elemA, elemB)
				: new ImmutablePair<>(elemB, elemA);
		if (elemB < elemA) fracA = 1.0 - fracA;

		// Get the convex hull
		List<ImmutablePair<Double,Double>> hull = ConvexHulls.get(binary);

		// Handle special cases
		if (hull == null) {
			throw new Error("Something has gone wrong. Find the author!");
		}
		if (hull.isEmpty())
			return 0.0;
		if (hull.size() == 1) {
			// Only one point on hull. Assume formation energy of elements are zero
			ImmutablePair<Double,Double> point = hull.get(0);
			if (fracA < point.getLeft()) {
				return safeInterpolate(fracA, 0.0, 0.0, point.getLeft(), point.getRight());
			} else {
				return safeInterpolate(fracA, point.getLeft(), point.getRight(), 1.0, 0.0);
			}
		}

		/** Calculate the point on the convex hull */
		ImmutablePair<Double,Double> leftPoint, rightPoint = new ImmutablePair<>(1.0,0.0);
		boolean foundRight = false;
		ListIterator<ImmutablePair<Double,Double>> iter = hull.listIterator();
		// Move until we have found the two neighboring compounds, or run out of room
		while (iter.hasNext()) {
			rightPoint = iter.next();
			if (rightPoint.getLeft() >= fracA) { 
				foundRight = true;
				break;
			}
		}
		if (foundRight) {
			// Go back and get the left point
			iter.previous();
			if (iter.hasPrevious()) leftPoint = iter.previous();
			else leftPoint = new ImmutablePair<>(0.0,0.0);
		} else {
			// Left point is point with x_A greatest
			leftPoint = rightPoint;
			// Right point is (1.0,0.0)
			rightPoint = new ImmutablePair<>(1.0, 0.0);
		}

		return safeInterpolate(fracA, leftPoint.getLeft(), leftPoint.getRight(), 
				rightPoint.getLeft(), rightPoint.getRight());
	}
	
	/**
	 * Safely interpolate a linear function. That is, don't allow division by zero.
	 * @param x Point at which function should be evaluated
	 * @param leftX X-value of left point (x should be > leftX)
	 * @param leftY Y-value of left point
	 * @param rightX X-value of right point
	 * @param rightY Y-value of right point
	 * @return (x - leftX) * (rightY - leftY) / (rightX - leftX)
	 */
	private double safeInterpolate(double x, double leftX, double leftY, double rightX, double rightY) {
		double output;
		if (x - leftX > 1e-4) {
			output = leftY + (x - leftX) * (rightY - leftY) / (rightX - leftX);
		} else {
			output = rightY + (x - rightX) * (rightY - leftY) / (rightX - leftX);
		}
		return output;
	}
	
	
	/**
	 * Predict the formation energy of a compound. Extrapolates from the binary
	 *  formation energies in a manner described in <a href="http://www.cambridge.org/us/academic/subjects/engineering/materials-science/phase-equilibria-phase-diagrams-and-phase-transformations-their-thermodynamic-basis-2nd-edition">
	 * Hillert's text</a>.
	 * 
	 * <p>Basic procedure:
	 * <ol>
	 * <li>Project composition onto each binary. Uses the Muggianu method. x<sup>proj</sup><sub>A</sub> = (1 + x<sub>A</sub> - x<sub>B</sub>)/2
	 * <li>Evaluate binary convex hull at projected points.
	 * <li>Calculate weight for contribution from each binary as 1.0/distance from that binary. 
	 * w = x<sub>A</sub>x<sub>B</sub>/(x<sup>proj</sup><sub>A</sub>(1-x<sup>proj</sup><sub>A</sub>))
	 * <li>Determine weighted average of all binary contributions
	 * </ol>
	 * @param compound Compound to be evaluated
	 * @return Predicted formation energy based on mixing model (see above)
	 */
	public double evaluateCompound(CompositionEntry compound) {
		int[] element = compound.getElements();
		
		// Handle special case: Only one element
		if (element.length == 1) {
			for (Map.Entry pair : ConvexHulls.entrySet()) {
				ImmutablePair<Integer,Integer> binary = (ImmutablePair<Integer,Integer>) pair.getKey();
				if (binary.getLeft() == element[0] || binary.getRight() == element[0]) {
					List<ImmutablePair<Double,Double>> hull = (List<ImmutablePair<Double,Double>>) pair.getValue();
					if (! hull.isEmpty()) {
						if (binary.getLeft() == element[0]) {
							// Should have x_left = 1.0
							ImmutablePair<Double,Double> point = hull.get(hull.size() - 1);
							return Math.abs(point.getLeft() - 1.0) < 1e-4 ? point.getRight() : 0.0;
						} else {
							// Should have x_left = 0.0
							ImmutablePair<Double,Double> point = hull.get(hull.size() - 1);
							return Math.abs(point.getLeft()) < 1e-4 ? point.getRight() : 0.0;
						}
					}
				}
			}
		}
		
		// Handle special case: Two elements
		double[] fraction = compound.getFractions(); 
		if (element.length == 2) {
			return evaluatePoint(element[0], element[1], fraction[0]);
		}
		
		// Handle normal case
		double output = 0.0, totalWeight = 0.0;
		for (int elemA=0; elemA<element.length; elemA++) {
			for (int elemB=elemA+1; elemB<element.length; elemB++) {
				// Project this into the binary
				double fracAproj = (1 + fraction[elemA] - fraction[elemB]) / 2;
				// Determine binary convex hull at that point
				double contrib = evaluatePoint(element[elemA], element[elemB], fracAproj);
				// Determine weight (Muggianu method)
				double weight;
				weight = fraction[elemA] * fraction[elemB] / (fracAproj * (1.0 - fracAproj));
				// Add weighted contribution to value
				totalWeight += weight;
				output += contrib * weight;
			}
		}
		
		return output / totalWeight;
	}

}
