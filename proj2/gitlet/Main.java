package gitlet;

import static gitlet.MyUtils.exit;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Exuanbo
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exit("Please enter a command.");
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init" -> {
                validateNumArgs(args, 1);
                Repository.init();
            }
            case "add" -> {
                validateNumArgs(args, 2);
                Repository.checkWorkingDir();
                String fileName = args[1];
                new Repository().add(fileName);
            }
            case "commit" -> {
                validateNumArgs(args, 2);
                Repository.checkWorkingDir();
                String message = args[1];
                if (message.length() == 0) {
                    exit("Please enter a commit message.");
                }
                new Repository().commit(message);
            }
            case "rm" -> {
                validateNumArgs(args, 2);
                Repository.checkWorkingDir();
                String fileName = args[1];
                new Repository().remove(fileName);
            }
            case "log" -> {
                validateNumArgs(args, 1);
                Repository.checkWorkingDir();
                new Repository().log();
            }
            case "global-log" -> {
                validateNumArgs(args, 1);
                Repository.checkWorkingDir();
                Repository.globalLog();
            }
            case "find" -> {
                validateNumArgs(args, 2);
                Repository.checkWorkingDir();
                String message = args[1];
                if (message.length() == 0) {
                    exit("Found no commit with that message.");
                }
                Repository.find(message);
            }
            default -> exit("No command with that name exists.");
        }
    }

    /**
     * Checks the number of arguments versus the expected number.
     *
     * @param args Argument array from command line
     * @param n    Number of expected arguments
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exit("Incorrect operands.");
        }
    }
}
