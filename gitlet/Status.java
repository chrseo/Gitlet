package gitlet;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/** Class that holds methods for displaying status of working dir
 *  when called.
 *  @author Chris Seo
 */
public class Status {

    /** Working directory, where user initializes gitlet. */
    static final File WORKING_DIR = Main.WORKING_DIR;

    /** Stores commit tree. */
    static final File TREE_DIR = Tree.TREE_DIR;

    /** Stores staged additions files. */
    static final File STAGED_FILES = Stage.STAGED_SAVE;

    /** Staged for removal directory. */
    static final File STAGE_RM_DIR = Stage.STAGE_RM_DIR;

    /** Staged for addition directory. */
    static final File STAGE_DIR = Stage.STAGE_DIR;

    /** Style lines for status. */
    static final String LINES = "===";

    /** Does the status command. */
    static void doStatus() {
        printBranches();
        printStaged();
        printRemoved();
        printModifications();
        printUntracked();
    }

    /** Prints branches of the current tree. */
    static void printBranches() {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        System.out.println(LINES + " Branches " + LINES);

        HashMap<String, Commit> branches = workingTree.getBranches();
        ArrayList<String> toPrint = new ArrayList<>(branches.keySet());
        Collections.sort(toPrint);
        for (int i = 0; i < toPrint.size(); i++) {
            if (toPrint.get(i).equals(workingTree.currentBranch())) {
                toPrint.set(i, "*" + workingTree.currentBranch());
            }
        }
        for (String el : toPrint) {
            System.out.println(el);
        }
        System.out.print("\n");
    }

    /** Prints the files staged for addition. */
    static void printStaged() {
        HashSet<String> stagedFiles = Utils.readObject(STAGED_FILES,
                Stage.class).getStagedFiles();
        System.out.println(LINES + " Staged Files " + LINES);
        ArrayList<String> toPrint = new ArrayList<>(stagedFiles);
        Collections.sort(toPrint);
        for (String el : toPrint) {
            System.out.println(el);
        }
        System.out.print("\n");
    }

    /** Prints the files staged for removal. */
    static void printRemoved() {
        System.out.println(LINES + " Removed Files " + LINES);
        ArrayList<String> toPrint = new ArrayList<>();
        for (File file : STAGE_RM_DIR.listFiles()) {
            if (!file.isDirectory()) {
                toPrint.add(file.getName());
            }
        }
        Collections.sort(toPrint);
        for (String el : toPrint) {
            System.out.println(el);
        }
        System.out.print("\n");
    }

    /** Prints the files that are modified or deleted. */
    static void printModifications() {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);

        HashSet<String> stagedRemove = Utils.filesSet(STAGE_RM_DIR);
        HashSet<String> staged = Utils.readObject(STAGED_FILES,
                Stage.class).getStagedFiles();

        System.out.println(LINES + " Modifications Not "
                + "Staged For Commit " + LINES);

        HashMap<String, Blob> currBlobs = workingTree.
                getCurrHead().getBlobs();

        ArrayList<String> blobs = new ArrayList<>(currBlobs.keySet());
        Collections.sort(blobs);

        HashSet<String> workingFiles = Utils.filesSet(WORKING_DIR);
        for (String fileName : blobs) {
            File workingFile = Utils.join(WORKING_DIR, fileName);
            if (!workingFiles.contains(fileName)
                    && !stagedRemove.contains(fileName)) {
                System.out.println(fileName + " (deleted)");
            } else if (workingFile.exists()) {
                if (!Utils.readContentsAsString(workingFile).
                        equals(currBlobs.get(fileName).getContents())
                        && !staged.contains(fileName)) {
                    System.out.println(fileName + " (modified)");
                }
            }
        }
        System.out.print("\n");
    }

    /** Prints the files that are untracked. */
    static void printUntracked() {
        Tree workingTree = Utils.readObject(TREE_DIR, Tree.class);
        System.out.println(LINES + " Untracked Files " + LINES);

        HashSet<String> untracked = Checkout.
                everythingTracked(workingTree.getCurrHead());
        ArrayList<String> almostUntracked = new ArrayList<>(untracked);
        Collections.sort(almostUntracked);
        HashSet<String> staged = Utils.readObject(STAGED_FILES,
                Stage.class).getStagedFiles();
        for (String file : almostUntracked) {
            if (!staged.contains(file)) {
                System.out.println(file);
            }
        }
    }
}
