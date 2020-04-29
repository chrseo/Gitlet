package gitlet;

import java.io.File;
import java.util.HashSet;

/** Handles commands related to viewing archived commits.
 *  @author Chris Seo
 */
public class Archive {

    /** Stores commit tree. */
    static final File TREE_DIR = Tree.TREE_DIR;

    /** Gitlet directory, where gitlet is stored. */
    static final File GITLET_DIR = Main.GITLET_DIR;

    public static void doLog(boolean global) {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        if (global) {
            for (String commitID : workingTree.getAllCommits()) {
                File commitFile = findFromGitletDir(commitID);
                Commit commit = Utils.readObject(commitFile, Commit.class);
                System.out.println("===\n"
                        + "commit " + commit.getID() + "\n"
                        + "Date: " + commit.getTimestamp() + "\n"
                        + commit.getMessage() + "\n");
            }
        } else {
            Commit currHead = workingTree.getCurrHead();
            while (currHead != null) {
                System.out.println("===\n"
                        + "commit " + currHead.getID() + "\n"
                        + "Date: " + currHead.getTimestamp() + "\n"
                        + currHead.getMessage() + "\n");
                currHead = currHead.getParent();
            }
        }
    }

    /** Finds and returns a file from the .gitlet directory.
     * @param fileName name of file to be returned
     * @return file */
    private static File findFromGitletDir(String fileName) {
        for (File file : GITLET_DIR.listFiles()) {
            if (!file.isDirectory()) {
                if (file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }

    /** Does the find command.
     * @param args takes find + message of commit */
    public static void doFind(String[] args) {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        HashSet<String> allCommits = workingTree.getAllCommits();
        String message = args[1];
        HashSet<String> result = new HashSet<>();
        for (File file : GITLET_DIR.listFiles()) {
            if (!file.isDirectory()) {
                if (allCommits.contains(file.getName())) {
                    Commit currCommit = Utils.readObject(file, Commit.class);
                    if (currCommit.getMessage().equals(message)) {
                        System.out.println(currCommit.getID());
                        result.add(currCommit.getID());
                    }
                }
            }
        }
        if (result.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }
}
