/**
 * Given a MagpieClient, get all models with a certain datatype
 * @param client [in|out] Connected MagpieServerClient
 * @param dataType [in] Desired data type
 * @return Map of name -> Model info for models that have the target type
 */
function getModelsOfType(client, dataType) {
    var allModels = client.getModelInformation();
    var output = new Array();
    for (var model in allModels) {
        if (allModels[model].dataType.indexOf(dataType)!== -1) {
            output[model] = allModels[model];
        }
    }
    return output;
}

/** 
 * Create a table to store output data. Looks something like
 * 
 *          oqmd_dH             oqmd_vol
 *          &Delta;H (eV/atom)  V (Ang^3/atom)
 * entry_1  model1_entry1       model2_entry1
 * entry_2  model1_entry2       model2_entry2
 * @param modelInfo [in] Information about all models
 * @param models    [in] Models to print
 * @param entries   [in] Entries to print
 * @return A table object, as specified
 */
function createTable(modelInfo, models, entries) {
    // Thanks to: http://stackoverflow.com/questions/14643617/create-table-using-javascript
    var tbl = document.createElement('table');
    var tbody = document.createElement('tbdy');
    tbl.appendChild(tbody)
    var tr = document.createElement('tr');
    
    // List out the model names
    var td = document.createElement('th');
    tr.appendChild(td);
    td.innerHTML = "Model:";
    for (model in models) {
        td = document.createElement('th');
        td.innerHTML = models[model]
        tr.appendChild(td)
    }
    tbody.appendChild(tr);
    
    // List out model properties
    var td = document.createElement('th');
    tr = document.createElement('tr');
    td.innerHTML = "Property:";
    tr.appendChild(td);
    for (model in models) {
        td = document.createElement('th');
        name = models[model];
        td.innerHTML = modelInfo[name].property + " (" + modelInfo[name].units + ")";
        tr.appendChild(td);
    }
    tbody.appendChild(tr);
    
    // Print out 
    for (i in entries) {
        var entry = entries[i]
        tr = document.createElement('tr');
        td = document.createElement('td');
        td.innerHTML = entry.name
        tr.appendChild(td)
        for (model in models) {
        	mname = models[model]
            td = document.createElement('td');
            var num = entry.predictedProperties[mname];
            if (entry.classProbs[mname] == undefined) {
	            td.innerHTML = num.toPrecision(3)
            } else {
            	td.innerHTML = modelInfo[mname].units.split(";")[num]
            	td.innerHTML += " ("
            	x = entry.classProbs[mname][num] * 100
            	td.innerHTML += x.toFixed(1)
            	td.innerHTML += "&#37;)"
            }
            tr.appendChild(td)
        }
        tbody.appendChild(tr)
    }
    
    return tbl;
}

/** 
 * Write out model information
 * 
 * @param modelInfo [in] Information about model
 * @param outputDiv [in] Part of page to output data
 */
function outputModelInfo(modelInfo, outputDiv) {
	    	
	if (modelInfo === undefined) {
		outputDiv.innerHTML += "<p>No such model.</p>";
	}
	outputDiv.innerHTML += "<p>" + modelInfo.notes + "</p>";
	
	// Make a table storing general information about model
	output = "<h3>General Information</h3>";
	output += "<label>Property</label></th><td>" + modelInfo.property + "</td>";
	output += "<br><label>Units</label></th><td>" + modelInfo.units + "</td>";
	output += "<br><label>Training Set</label></th><td>" + modelInfo.training + "</td>";
	output += "<br><label>Training Time</label>" + modelInfo.trainTime;
	output += "<br><label>Author</label></th><td>" + modelInfo.author + "</td>";
	output += "<br><label>Citation</label></th><td>" + modelInfo.citation + "</td>";
			
	outputDiv.innerHTML += output;
	
	// Print out model performance
	output = "<h3>Model Performance</h3>"
	output += "<p><label>Validation Method</label>" + modelInfo.valMethod + "</p>" 
	if (modelInfo.classifier) {
		output += "<label>Accuracy</label>" + (modelInfo.valScore.Accuracy * 100).toFixed(2) + "%"
		output += "<br><label>Kappa</label>" + modelInfo.valScore.Kappa.toFixed(4)
		output += "<br><label>Sensitivity</label>" + (modelInfo.valScore.Sensitivity * 100).toFixed(2) + "%"
		output += "<br><label>False Positive Rate</label>" + (modelInfo.valScore.FPR * 100).toFixed(2) + "%"
		output += "<br><label>Area Under ROC</label>" + modelInfo.valScore.ROCAUC.toFixed(4)
	} else {
		output += "<label>R</label>" + modelInfo.valScore.R.toFixed(4)
		output += "<br><label>&rho;</label>" + modelInfo.valScore.Rho.toFixed(4)
		output += "<br><label>&tau;</label>" + modelInfo.valScore.Tau.toFixed(4)
		output += "<br><label>MAE</label>" + modelInfo.valScore.MAE.toPrecision(3) + " " + modelInfo.units
		output += "<br><label>RMSE</label>" + modelInfo.valScore.RMSE.toPrecision(3) + " " + modelInfo.units
		if (modelInfo.valScore.MRE != "NaN") {
			output += "<br><label>MRE</label>" + (modelInfo.valScore.MRE * 100).toFixed(2) + "%"
		}
	}
	
	outputDiv.innerHTML += output

	// Print out information about dataset
	output = "<h3>Attribute Information</h3>\n";
	dataInfo = modelInfo.dataType.split("\n");
	for (i in dataInfo) {
		line = dataInfo[i]
		
		words = line.split(" ")
	
		// Look for names of magpie classes
		for (j in words) {
			word = words[j]
			if (word.indexOf("magpie.") == 0) {
				words[j] = getDocReference(words[j])
			}
		}
		
		// Output line
		for (j in words) {
			output += " " + words[j]
		}
	}
	outputDiv.innerHTML += output;
	
	// Print out model structure
	output = "<h3>Model Structure</h3>"
	output += "<div style=\"font-size: 12px\">"
	typeLines = modelInfo.modelType.split("\n")
	for (i in typeLines) {
		line = typeLines[i]
		
		words = line.split(" ")
	
		// Look for names of magpie classes
		for (j in words) {
			word = words[j]
			if (word.indexOf("magpie.") == 0) {
				words[j] = getDocReference(words[j])
			}
		}
		
		// Output line
		for (j in words) {
			output += " " + words[j]
		}
		
	}
	output += "</div>"
	
	outputDiv.innerHTML += output
}

var javadocURL = "http://oqmd.org/static/analytics/magpie/javadoc/"
/**
 * Get weblink to a certain magpie class
 * @param className [in] Full name of magpie class (should start with "magpie.")
 * @return Hyperlink to its documentation page
 */
function getDocReference(className) {
	return "<a href=\"" + javadocURL + className.replace(/\./g,"/") + ".html\">" + className.replace("magpie.","") + "</a>"
}
