/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.utility;

import magpie.data.Dataset;
import magpie.models.BaseModel;

/** 
 * This class serves a sole purpose: asynchronously run a model. Still under development, I would 
 * not recommend using this.
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class ModelRunningThread implements Runnable {
    public final BaseModel Model;
    public Dataset Data;
    
    @Override public void run() {
        Model.run_protected(Data);
    }

    public ModelRunningThread(BaseModel Model, Dataset Data) {
        this.Model = Model;
        this.Data = Data;
    }
}
