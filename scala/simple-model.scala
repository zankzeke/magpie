/**
 * Create a RandomForest model for formation enthalpy, test it
 *  using 10-fold CV.
 *
 * Same as examples/simple-model.in
 */

import magpie.data.materials.CompositionDataset
import magpie.models.regression.WekaRegression

// Load in dataset
val data = new CompositionDataset()
var filename = "datasets/small_set.txt"
data.importText(filename, null)
println(s"Read in ${data.NEntries()} from $filename")

// Set formation enthalpy as property to be modelled
data.setTargetProperty("delta_e", true)
println(s"Set ${data.getTargetPropertyName()} as property to be modeled")

// Generate attributes
data.addElementalPropertySet("general")
data.generateAttributes()
println(s"Generated ${data.NAttributes()} attributes")

// Train a Weka REPTree
val model = new WekaRegression("trees.RandomForest", "-I 110 -K 7".split(" "))
model.train(data)
model.crossValidate(10, data, 100)

println("\nValidated model using 10-fold CV")
println("Performance statistics:")
model.ValidationStats.toString().split("\n").map("\t" + _).foreach(println)
