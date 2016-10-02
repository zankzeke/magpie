package magpie.models.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for models that run their model in a separate thread and communicate
 * to it via a socket connection. Assumes that the model object is communicated
 * over some kind of stream, and stored inside the Magpie Java object.
 * 
 * @author Logan Ward
 */
public interface ExternalModel extends AutoCloseable {
    
    /**
     * Start the server hosting the model. This process will initialize
     * the server and set the communication port number.
     *
     * @throws java.lang.Exception
     */
    public void startServer() throws Exception;
    
    /**
     * Get the process holding this external port
     * @return 
     */
    public Process getProcess();
    
    /**
     * Check whether the server is running
     * @return Whether server is running
     */
    public boolean serverIsRunning();
    
    /**
     * Get the port number for this model's server
     * @return Port number. If server is not running, this is meaningless
     */
    public int getPort();
    
    /**
     * Set a command to the model server that we are done with it.
     * 
     * Use when program is done with a model, and needs to clear up port
     */
    public void closeServer();

    /**
     * Read file describing model into memory
     *
     * @param input Input stream providing model data
     * @throws java.io.IOException
     */
    void readModel(InputStream input) throws Exception;

    /**
     * Define how well model file is compressed
     *
     * @param level Desired level. 1: Fastest, 9: Smallest memory footprint
     * @throws Exception
     * @see #ScikitModel
     */
    void setCompressionLevel(int level) throws Exception;

    /**
     * Write model to an output stream
     *
     * @param output
     * @throws java.io.IOException
     */
    void writeModel(OutputStream output) throws IOException;
}
