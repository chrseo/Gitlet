package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/** Class that contains file blobs, a commit message, references to parents,
 *  and a timestamp of creation.
 *  @author Chris Seo
 */
public class Commit implements Serializable {

    static final String COMMIT_STR = "commit";

    public Commit(String timestamp, String message, String parent,
                  HashMap<String, Blob> blobs) {
        _timestamp = timestamp;
        _message = message;
        _parent = parent;
        _blobs = blobs;
        generateID();
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public String getMessage() {
        return _message;
    }

    public String getParentID() {
        return _parent;
    }

    public HashMap<String, Blob> getBlobs() {
        return _blobs;
    }

    public void generateID() {
        byte[] thisAsBytes = Utils.serialize(this);
        _identifier = Utils.sha1(thisAsBytes, COMMIT_STR);
    }

    public String getID() {
        return _identifier;
    }

    private String _parent;
    private String _identifier;
    private String _timestamp;
    private String _message;
    private HashMap<String, Blob> _blobs;
}
