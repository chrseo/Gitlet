package gitlet;

import java.io.File;
import java.io.Serializable;

/** Blob class that holds contents and ID for a file.
 *  @author Chris Seo
 */
public class Blob implements Serializable {

    static final String BLOB_STR = "blob";

    public Blob(File file) {
        _contents = Utils.readContentsAsString(file);
        _name = file.getName();
        generateID();
    }

    public String getName() {
        return _name;
    }

    public void generateID() {
        _identifier = Utils.sha1(_contents, BLOB_STR);
    }

    public String getID() {
        return _identifier;
    }

    public String getContents() {
        return _contents;
    }

    private String _name;
    private String _identifier;
    private String _contents;
}
