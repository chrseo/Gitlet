package gitlet;

import java.io.File;
import java.io.Serializable;

/** Blob class that holds contents and ID for a file.
 *  @author Chris Seo
 */
public class Blob implements Serializable {

    /** String to be hashed in with contents for ID */
    static final String BLOB_STR = "blob";

    /** Constructor for Blob class. */
    public Blob(File file) {
        _contents = Utils.readContentsAsString(file);
        _name = file.getName();
        generateID();
    }

    /** Returns name of blob. */
    public String getName() {
        return _name;
    }

    /** Creates an ID for the blob. */
    public void generateID() {
        _identifier = Utils.sha1(_contents, BLOB_STR);
    }

    /** Returns the contents of the blob. */
    public String getContents() {
        return _contents;
    }

    /** Name of blob. */
    private String _name;

    /** ID for blob. */
    private String _identifier;

    /** Contents of blob. */
    private String _contents;
}
