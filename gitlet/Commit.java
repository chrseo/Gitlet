package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/** Class that contains file blobs, a commit message, references to parents,
 *  and a timestamp of creation.
 *  @author Chris Seo
 */
public class Commit implements Serializable {

    static final String COMMIT_STR = "commit";
    static final String INIT_DATE = "Wed Dec 31 16:00:00 1969 -0800";
    static final File GITLET_DIR = Main.GITLET_DIR;

    public Commit(String message, String parent,
                  HashMap<String, Blob> blobs, boolean isMerge,
                                                boolean isInitial) {
        if (isInitial) {
            _timestamp = INIT_DATE;
        } else {
            String date = DateTimeFormatter.ofPattern("EEE " + "LLL " + "dd "
                    + "HH:mm:ss " + "yyyy " + "Z").
                    format(ZonedDateTime.now());
            _timestamp = date;
        }
        _parent2 = null;
        _message = message;
        _parent = parent;
        _blobs = blobs;
        _isMerge = isMerge;
        generateID();
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public String getMessage() {
        return _message;
    }

    public Commit getParent() {
        if (_parent == null) {
            return null;
        }
        File file = Utils.join(GITLET_DIR, _parent);
        return Utils.readObject(file, Commit.class);
    }

    public Commit getParent2() {
        if (_isMerge) {
            File file = Utils.join(GITLET_DIR, _parent2);
            return Utils.readObject(file, Commit.class);
        }
        return null;
    }

    public void setParent2ID(String parent2ID) {
        _parent2 = parent2ID;
    }

    public boolean isMerge() {
        return _isMerge;
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
    private String _parent2;
    private boolean _isMerge;
    private String _identifier;
    private String _timestamp;
    private String _message;
    private HashMap<String, Blob> _blobs;
}
