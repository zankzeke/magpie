/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.materials.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.MultiPropertyEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Holds static operations that permit writing composition datasets to disk.
 * @author Logan Ward
 */
public class CompositionDatasetOutput {
    
    /**
     * Writes a comma-delimited file containing measured and predicted properties.
     *  Format is something like:
     * 
     * <center>X_Element#1, X_Element#2, Property1_Measured, Property1_Predicted, [...]</center>
     * 
     * <p>Elements are chosen to be only those represented in the dataset and 
     * listed in ascending Z
     * @param data Dataset to be printed
     * @param filename Path to desired output
     */
    static public void saveCompositionProperties(CompositionDataset data, String filename) {
        // Open the file writer
        PrintWriter fp;
        try {
            fp = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
        } catch (IOException e) {
            throw new Error(e);
        }
        
        // Get list of elements
        Set<Integer> elems = new TreeSet<>();
        for (BaseEntry e : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) e;
            for (int elem : entry.getElements()) {
                elems.add(elem);
            }
        }
        
        // Print out the header
        String[] elemNames = data.ElementNames;
        for (int elem : elems) {
            fp.print(elemNames[elem] + ",");
        }
        for (String property: data.getPropertyNames()) {
            fp.print(String.format("%s_measured,%s_predicted,", property, property));
        }
        fp.println();
        
        // Print out the data
        for (BaseEntry e : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) e;
            for (int elem : elems) {
                fp.print(String.format("%.4f,", entry.getElementFraction(elem)));
            }
            MultiPropertyEntry ptr = (MultiPropertyEntry) entry;
            for (int i=0; i<data.NProperties(); i++) {
                String m = ptr.hasMeasuredProperty(i) ? 
                        String.format("%.7e", ptr.getMeasuredProperty(i)) : "None";
                String p = ptr.hasPredictedProperty(i) ? 
                        String.format("%.7e", ptr.getPredictedProperty(i)) : "None";
                fp.format("%s,%s,",m,p);
            }
            fp.println();
        }
        fp.close();
    }
}
