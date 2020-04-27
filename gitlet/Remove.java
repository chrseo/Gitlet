package gitlet;

import java.io.File;
import java.util.HashSet;

public class Remove {

    static final File STAGE_DIR = Stage.STAGE_DIR;

    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    static final File STAGED_FILES = Stage.STAGED_SAVE;

    static final File TREE_DIR = Tree.TREE_DIR;

    static final File WORKING_DIR = Main.WORKING_DIR;

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
            addToStageRemoval(fileName);
            removeFromWorking(fileName);
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    private static void addToStageRemoval(String fileName) {
        File stagedFile = Utils.join(WORKING_DIR, fileName);
        File dest = Utils.join(STAGE_RM_DIR, fileName);
        Utils.copy(stagedFile, dest);
    }

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
