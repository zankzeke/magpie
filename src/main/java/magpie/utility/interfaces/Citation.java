package magpie.utility.interfaces;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Class used by {@linkplain Citable} to define a resource being cited.
 *
 * @author Logan Ward
 */
public class Citation {
    /** 
     * Component this citation is associated with
     */
    final public Class Component;
    
    /**
     * Author(s) associated with this citation
     */
    final public String[] Authors;
    
    /**
     * Title of resource being cited. Examples: Title of paper, name of software
     * package.
     */
    final public String Title;
    /**
     * Type of resource being cited. Examples: "Article", "Webpage", "Book",
     * "Thesis"
     */
    final public String Type;
    /**
     * URL of resource being cited. If not available, list None
     */
    final public URL Location;
    /**
     * Anything else that a user should know about this resource.
     */
    final public String Notes;

    /**
     * Create a new citation object. Optional arguments can be null. This should
     * give enough information for the person to find the resource but need not
     * include enough information for a complete citation.
     *
     * @param component Component that this citation is associated with
     * @param authors Optional: List of authors. [0] should be the primary
     * author. Preferred format: &lt;first initial&gt;. &lt;family name&gt;. 
     * Recommended to use "et al." for author lists longer than 3.
     * @param title Title of resource
     * @param type Type of resource being cited (e.g., "Article", "Webpage",
     * "Book").
     * @param url Optional: URL of resource
     * @param notes Optional: Anything else someone who cites this should know
     */
    public Citation(Class component, String type, String[] authors, String title,
            String url, String notes) {
        if (component == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }
        this.Component = component;
        
        // Get the type
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        this.Type = type;
        
        // Get the author list
        if (authors != null) {
            this.Authors = authors.clone();
        } else {
            this.Authors = null;
        }   
        
        // Get the title
        if (title == null) {
            throw new IllegalArgumentException("Title cannot be null");
        }
        this.Title = title;
        
        // Figure out the URL
        URL temp = null;
        try {
            if (url != null) {
                temp = new URL(url);
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad URL: " + url);
        }
        this.Location = temp;
        
        // Save any notes
        this.Notes = notes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Citation) {
            Citation x = (Citation) obj;
            if (!Objects.equals(x.Title, Title)) {
                return false;
            }
            if (!x.Component.equals(Component)) {
                return false;
            }
            return x.Type.equals(Type);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Component.hashCode() + Title.hashCode() + Type.hashCode();
    }

    /**
     * Print out a description of this citation.
     *
     * @return String containing a formated description of this citation
     */
    public String printInformation() {
        String output;
        
        // Print out component to which this citation belongs
        output = "Component: " + Component.getCanonicalName();
        
        // Print out infomration about citation
        output += "\nType: " + Type;

        if (Authors != null) {
            output += "\nAuthor";
            if (Authors.length > 1) {
                output += "s";
            }
            output += ": ";
            output += Authors[0];
            for (int i = 1; i < Authors.length; i++) {
                output += ", " + Authors[i];
            }
        }

        output += "\nTitle: " + Title;
        output += "\nURL: " + (Location != null ? Location.toString()
                : "Unknown");

        // Add in any notes
        if (Notes != null) {
            output += "\nNotes: " + Notes;
        }
        return output;
    }

    @Override
    public String toString() {
        if (Location != null) {
            return String.format("Component: %s - Type: %s - URL: %s",
                    Component.getName(),
                    Type, Location.toString());
        } else {
            return String.format("Component: %s - Type: %s",
                    Component.getName(),
                    Type);
        }
    }

    /**
     * Return citation into JSON. Detailed formatting should be in HTML format
     *
     * @return JSON object describing the citation
     */
    public JSONObject toJSON() {
        JSONObject output = new JSONObject();

        output.put("component", Component.getCanonicalName());
        output.put("authors", Authors);
        output.put("title", Title);
        if (Location != null) {
            output.put("url", Location.toString());
        }
        if (Type != null) {
            output.put("type", Type);
        }
        if (Notes != null) {
            output.put("notes", Notes);
        }

        return output;
    }
}
