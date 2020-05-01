# gitlet
Version control system that implements the basic features of Git.

## Command Line Usage
In directory where Gitlet is installed:  
`java gitlet.Main [command]`

## Commands
`init`: Initializes the .gitlet repository.  
`add [file name]`: Add a file to be staged for addition.  
`commit [message]`: Saves a snapshot of current commit and staging area in a new commit.  
`rm [file name]`: Unstage a file if currently staged for addition and/or untrack if in the current commit.  
`log`: Displays all commits in current branch.  
`global-log`: Displays all commits ever made.  
`find [commit message]`: Finds a commit with the given message.  
`status`: Displays gitlet's status of the working directory.  
`checkout -- [file name]`: Checks out a file in the current commit.  
`checkout [commit ID] -- [file name]`: Checks out a file in the given commit.  
`checkout [branch name]`: Checks out a given branch.  
`branch [name]`: Creates a new branch.  
`rm-branch [name]`: Removes a branch.  
`reset [commit ID]`: Resets the working directory to a given commit.  
`merge [branch name]`: Merges the current and given branch.  
