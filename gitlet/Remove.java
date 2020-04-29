package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/** Handles merge command.
 *  @author Chris Seo
 */
public class Remove {

    /** Staged for addition directory. */
    static final File STAGE_DIR = Stage.STAGE_DIR;

    /** Staged for removal directory. */
    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    /** Stores staged additions files. */
    static final File STAGED_FILES = Stage.STAGED_SAVE;

    /** Stores commit tree. */
    static final File TREE_DIR = Tree.TREE_DIR;

    /** Working directory, where user initializes gitlet. */
    static final File WORKING_DIR = Main.WORKING_DIR;

    /** Handles the remove command.
     * @param fileName takes a file to be removed */
    public static void doRemove(String fileName) {
        Stage fromSave = Utils.readObject(STAGED_FILES,
                Stage.class);
        HashSet<String> stagedFiles = fromSave.getStagedFiles();
        Commit currHead = Utils.readObject(TREE_DIR, Tree.class).
                getCurrHead();
        if (stagedFiles.contains(fileName) && currHead.getBlobs().
                containsKey(fileName)) {
            File stagedFile = Utils.join(STAGE_DIR, fileName);
            addToStageRemoval(fileName);
            removeFromWorking(fileName);
            stagedFile.delete();

            stagedFiles.remove(fileName);
            Stage.save(fromSave);
        } else if (stagedFiles.contains(fileName) && !currHead.getBlobs().
                containsKey(fileName)) {
            File stagedFile = Utils.join(STAGE_DIR, fileName);
            stagedFile.delete();

            stagedFiles.remove(fileName);
            Stage.save(fromSave);
        } else if (!stagedFiles.contains(fileName) && currHead.getBlobs().
                containsKey(fileName)) {
            if (!Utils.filesSet(WORKING_DIR).contains(fileName)) {
                File removedFile = Utils.join(STAGE_RM_DIR, fileName);
                try {
                    removedFile.createNewFile();
                } catch (IOException ignored) {
                    return;
                }
                String contents = currHead.getBlobs().get(fileName).
                        getContents();
                Utils.writeContents(removedFile, contents);
            } else {
                addToStageRemoval(fileName);
                removeFromWorking(fileName);
            }
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    /** Adds file to stage removal.
     * @param fileName name of file to be removed */
    private static void addToStageRemoval(String fileName) {
        File stagedFile = Utils.join(WORKING_DIR, fileName);
        File dest = Utils.join(STAGE_RM_DIR, fileName);
        Utils.copy(stagedFile, dest);
    }

    /** Deletes the file from the working directory.
     * @param fileName name of file to be deleted */
    private static void removeFromWorking(String fileName) {
        for (File file : WORKING_DIR.listFiles()) {
            if (!file.isDirectory()) {
                if (file.getName().equals(fileName)) {
                    file.delete();
                    break;
                }
            }
        }
    }
}
