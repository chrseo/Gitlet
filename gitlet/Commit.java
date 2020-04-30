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

    /** Message of initial commit. */
    static final String COMMIT_STR = "commit";

    /** Date of initial commit. */
    static final String INIT_DATE = "Wed Dec 31 16:00:00 1969 -0800";

    /** Gitlet directory, where gitlet is stored. */
    static final File GITLET_DIR = Main.GITLET_DIR;

    /** Constructor for commit.
     * @param message message of commit
     * @param parent parent's ID
     * @param blobs blobs stored
     * @param isMerge true if commit is a merge commit
     * @param isInitial true if commit is initial commit */
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

    /** Return timestamp. */
    public String getTimestamp() {
        return _timestamp;
    }

    /** Return the commit message. */
    public String getMessage() {
        return _message;
    }

    /** Return the parent of the commit. */
    public Commit getParent() {
        if (_parent == null) {
            return null;
        }
        File file = Utils.join(GITLET_DIR, _parent);
        return Utils.readObject(file, Commit.class);
    }

    /** Return the merged-in parent of the commit. */
    public Commit getParent2() {
        if (_isMerge) {
            File file = Utils.join(GITLET_DIR, _parent2);
            return Utils.readObject(file, Commit.class);
        }
        return null;
    }

    /** Set the merged in parent.
     * @param parent2ID merged in parent iD */
    public void setParent2ID(String parent2ID) {
        _parent2 = parent2ID;
    }

    /** Return true if commit is a merge commit. */
    public boolean isMerge() {
        return _isMerge;
    }

    /** Return the blobs of the commit. */
    public HashMap<String, Blob> getBlobs() {
        return _blobs;
    }

    /** Generate the commit ID. */
    public void generateID() {
        byte[] thisAsBytes = Utils.serialize(this);
        _identifier = Utils.sha1(thisAsBytes, COMMIT_STR);
    }

    /** Returns the ID of this commit. */
    public String getID() {
        return _identifier;
    }

    /** Parent of the commit. */
    private String _parent;

    /** Merged in parent of the commit. */
    private String _parent2;

    /** Whether commit is merge or not. */
    private boolean _isMerge;

    /** ID of commit. */
    private String _identifier;

    /** Timestamp of the commit. */
    private String _timestamp;

    /** Commit's message. */
    private String _message;

    /** Blobs of the commit. */
    private HashMap<String, Blob> _blobs;
}
