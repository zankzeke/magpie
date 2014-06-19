/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.optimization.rankers.EntryRanker;
import org.apache.commons.math3.stat.StatUtils;

/**
 * This class contains operations relating to basic outputs/conversions of the Dataset
 * @author Logan Ward
 * @version 0.1
 */
abstract public class DatasetOutput extends Dataset {
    /**
     * Saves a Dataset as a string-delimited file. First row of file is the name 
     * of each attribute, subsequent lines are each attribute for each entry. 
     * Measured class is printed, if available.
     * @param Data Dataset to be printed
     * @param Filename Desired filename
     * @param Delimiter Delimiter between words
     */
    static public void saveDelimited(Dataset Data, String Filename,  String Delimiter) {
        try {
            PrintWriter fp = new PrintWriter(new BufferedWriter(new FileWriter(Filename)));
            boolean hasMeasuredClass = Data.getEntry(0).hasMeasurement();
            // Print out the first row
            String[] attributeNames = Data.getAttributeNames();
            for (int i=0; i<Data.NAttributes(); i++)
                fp.format("%s%s", attributeNames[i], Delimiter);
            if (hasMeasuredClass)
                fp.println("Class");
            
            // Print out the data
            for (int i=0; i<Data.NEntries(); i++) {
                BaseEntry e = Data.getEntry(i);
                double[] attributes = e.getAttributes();
                for (int j=0; j < attributes.length; j++)
                    fp.format("%.7e%s", attributes[j], Delimiter);
                if (hasMeasuredClass)
                    fp.format("%.7e\n", e.getMeasuredClass());
            }
            fp.close();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    /**
     * Print top entries in a dataset based on an objective function
     * @param Data Dataset containing entries to be ranked
     * @param Ranker Method used to rank entries
     * @param Number Number of entries to print
     * @return A string displaying the results of the entry ranking
     */
    static public String printTopEntries(Dataset Data, EntryRanker Ranker, int Number) {
        double[] values = new double[Data.NEntries()];
        int[] rank = Ranker.rankEntries(Data, values);
        
        // Prepare the header
        String output= String.format("%5s\t%32s", "Rank", "Entry");
        if (Ranker.UseMeasured) output += String.format("\t%10s","Measured");
        else output += String.format("\t%10s","Predicted");
        
        // Get the predicted/measured value, if available
        boolean extra = false;
        double[] extra_values = null;
        if (Ranker.UseMeasured) {
            if (Data.getEntry(0).hasPrediction()) {
                extra = true;
                output += String.format("\t%10s","Predicted");
                Ranker.UseMeasured = false;
                extra_values = Ranker.runObjectiveFunction(Data);
                Ranker.UseMeasured = true;
            }
        } else {
            if (Data.getEntry(0).hasPrediction()) {
                extra = true;
                output += String.format("\t%10s","Measured");
                Ranker.UseMeasured = true;
                extra_values = Ranker.runObjectiveFunction(Data);
                Ranker.UseMeasured = false;
            }
        }
        output+="\n";

        // Print out the top entries
        double classv;
        for (int i=0, id; i<Number; i++) {
            id = rank[i];
            output+= String.format("#%4d\t%32s\t%10.4e", i, Data.getEntries().get(id),
                    values[i]);
            if (extra) {
                output+=String.format("\t%10.4e", extra_values[id]);
            }
            output+="\n";
        }
        return output;
    }

    /**
     * Write out to an ARFF-formatted file. See WEKA documentation
     *
     * @param filename Name of output file
     * @param Data Dataset to be outputted
     */
    static public void saveARFF(Dataset Data, String filename) {
        try {
            PrintWriter fp;
            fp = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            fp.write("@RELATION \'Auto-generated arff\'\n\n");
            for (int i = 0; i < Data.NAttributes(); i++) {
                fp.format("@ATTRIBUTE %s NUMERIC\n", Data.getAttributeName(i));
            }
            if (Data.NClasses() == 1) {
                fp.write("@ATTRIBUTE class NUMERIC\n");
            } else {
                String[] ClassNames = Data.getClassNames();
                fp.format("@ATTRIBUTE class {%s", ClassNames[0]);
                for (int i = 1; i < Data.NClasses(); i++) {
                    fp.format(", %s", ClassNames[i]);
                }
                fp.write("}\n");
            }
            fp.println("@DATA");
            for (int i = 0; i < Data.NEntries(); i++) {
                for (int j = 0; j < Data.NAttributes(); j++) {
                    fp.format("%.6e,", Data.getEntry(i).getAttribute(j));
                }
                if (Data.NClasses() == 1) {
                    fp.format("%.6e\n", Data.getEntry(i).getMeasuredClass());
                } else {
                    fp.format("%s\n", Data.getClassName((int) Data.getEntry(i).getMeasuredClass()));
                }
            }
            fp.close();
        } catch (IOException i) {
            throw new Error(i);
        }
    }
    
    /**
     * Print out the measured and predicted class for each entry in a dataset.
     * @param Data Dataset to be printed
     * @param filename Filename for the output
     */
    static public void printForStatistics(Dataset Data, String filename) {
        try {
            PrintWriter fp = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            fp.print("name, measured, predicted");
            if (Data.NClasses() > 1) fp.println(", probability,");
            else fp.println();                
            for (int i=0; i<Data.NEntries(); i++) {
                BaseEntry E = Data.getEntry(i);
                fp.format("\"%s\", %.7e, %.7e", E, E.getMeasuredClass(), E.getPredictedClass());
                if (Data.NClasses() > 1) 
                    fp.format(", %.7e\n", E.getClassProbilities()[(int) E.getPredictedClass()]);
                else fp.println();
            }
            fp.close();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    /**
     * Print out the measured and predicted class for each entry in a dataset. (tab delimited)
     * @param Data Dataset to be printed
     * @param filename Filename for the output
     */
    static public void printForStatistics_Tab(Dataset Data, String filename) {
        try {
            PrintWriter fp = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            fp.print("name\tmeasured\tpredicted");
            if (Data.NClasses() > 0) fp.println("\tprobability\t");
            else fp.println();                
            for (int i=0; i<Data.NEntries(); i++) {
                BaseEntry E = Data.getEntry(i);
                fp.format("%s\t%.7e\t%.7e", E, E.getMeasuredClass(), E.getPredictedClass());
                if (Data.NClasses() > 1) 
                    fp.format("\t%.7e\n", E.getClassProbilities()[(int) E.getPredictedClass()]);
                else fp.println();
            }
            fp.close();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    

    /**
     * Print out statistics for each feature to a datafile
     * @param Data Dataset to be printed
     * @param filename Desired filename
     */
    static public void printStatistics(Dataset Data, String filename) {
        if (Data == null) return;
        if (Data.NEntries() == 0) return;
        try { 
            PrintWriter fp = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            fp.println("Attribute, Min, LQR, Median, UPQ, Max, Mean, StdDev,");
            double x; double[] data;
            // Print out data about each feature.
            for (int i = 0; i < Data.NAttributes(); i++) {
                fp.print(Data.getAttributeName(i)+",");
                data = Data.getSingleAttributeArray(i);
                x = StatUtils.min(data); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 25); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 50); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 75); fp.format("%.5e,", x);
                x = StatUtils.max(data); fp.format("%.5e,", x);
                x = StatUtils.mean(data); fp.format("%.5e,", x);
                x = StatUtils.variance(data, x); fp.format("%.5e,", x);
                fp.println();
            }
            // Print out data about measured/predicted class
            if (Data.getEntry(0).hasMeasurement()) { 
                data = Data.getMeasuredClassArray();
                fp.print("MeasuredClass,");
                x = StatUtils.min(data); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 25); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 50); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 75); fp.format("%.5e,", x);
                x = StatUtils.max(data); fp.format("%.5e,", x);
                x = StatUtils.mean(data); fp.format("%.5e,", x);
                x = StatUtils.variance(data, x); fp.format("%.5e,", x);
                fp.println();
            }
            if (Data.getEntry(0).hasPrediction()) {
                data = Data.getPredictedClassArray();
                fp.print("PredictedClass,");
                x = StatUtils.min(data); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 25); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 50); fp.format("%.5e,", x);
                x = StatUtils.percentile(data, 75); fp.format("%.5e,", x);
                x = StatUtils.max(data); fp.format("%.5e,", x);
                x = StatUtils.mean(data); fp.format("%.5e,", x);
                x = StatUtils.variance(data, x); fp.format("%.5e,", x);
                fp.println();                
            }
            fp.close();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
