
package magpie.attributes.selectors;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.*;
import magpie.data.Dataset;

/**
 * Selects attributes based on whether their name matches a regular expression.
 * 
 * <usage><p><b>Usage</b>: [-v] &lt;Regex&gt;
 * <br><pr><i>-v</i>: Exclude attributes whose name matches this Regex
 <br><pr><i>Regex</i>: Regular expression designed to match attributes to be included.
 * <br>If the Regex includes spaces, surround with ""s</usage>
 * 
 * @author Logan Ward
 */
public class RegexAttributeSelector extends BaseAttributeSelector {
    /** Regular expression to be matched */
    protected Pattern Regex;
    /** Whether to include attributes that match */
    protected boolean IncludeMatching = true;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String regex;
        try {
            if (Options.size() == 2) {
                if (Options.get(0).toString().equalsIgnoreCase("-v")) {
                    setIncludeMatching(false);
                } else {
                    throw new Exception();
                }
                regex = Options.get(1).toString();
            } else if (Options.size() == 1) {
                regex = Options.get(0).toString();
                setIncludeMatching(true);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setRegex(regex);
    }

    @Override
    public String printUsage() {
        return "Usage: [-v] <regex>";
    }
    
    /**
     * Define the regular expression.
     * @param regex Regular expression to be matched
     * @throws PatternSyntaxException 
     */
    public void setRegex(String regex) throws PatternSyntaxException {
        this.Regex = Pattern.compile(regex);
		trained = false;
    }

    /**
     * Set whether to include (or exclude) attributes whose name matches the pattern.
     * @param x Desired operation
     */
    public void setIncludeMatching(boolean x) {
        this.IncludeMatching = x;
		trained = false;
    }

    @Override
    protected List<Integer> train_protected(Dataset Data) {
        List<Integer> output = new LinkedList<>();
        for (int i=0; i<Data.NAttributes(); i++) {
            String name = Data.getAttributeName(i);
            boolean matches = Regex.matcher(name).matches();
            if (matches == IncludeMatching) {
                output.add(i);
            }
        }
        return output;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = "Select attributes that ";
        
        output += IncludeMatching ? "match" : "do not match";
        
        output += " the regex: " + Regex;
        
        return output;
    }
}
