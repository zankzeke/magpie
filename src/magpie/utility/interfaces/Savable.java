package magpie.utility.interfaces;

/**
 * Dictates commands that must be fulfilled for an object to be "savable".
 * @author Logan Ward
 * @version 0.1
 */
public interface Savable {
    
    /**
     * Handles complicated saving commands.
	 * 
	 * <p>Dev Note: Make sure to add save format to Javadoc. See {@linkplain magpie.data.Dataset} as an example.
	 * Required format:
	 * 
	 * <p>&lt;save&gt;&lt;p&gt;&lt;b&gt;format&lt;b&gt; - Description
	 * <br>&lt;br&gt;Optional room to talk more about format &lt;/save&gt;
     * @param Basename Name of file without extension
     * @param Format Command specifying format in which to print
     * @return Filename Path to output file
     * @throws Exception If command not understood
     */
    abstract public String saveCommand(String Basename, String Format) throws Exception;
}
