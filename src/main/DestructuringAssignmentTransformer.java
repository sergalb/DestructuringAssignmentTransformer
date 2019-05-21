package main;

import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.tree.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DestructuringAssignmentTransformer {

    public static void main(String[] args) throws Exception {
        if (args == null) {
            System.err.println("need 2 arguments - input and output files names, now - 0");
            return;
        } else if (args.length != 2) {
            System.err.println("need 2 arguments - input and output files names, now - " + args.length);
            return;
        } else if (args[0] == null | args[1] == null) {
            System.err.println("args is null");
            return;
        }
        transformFile(args[0], args[1]);
    }

    public static void transformFile(String inputFile, String outputFile){
        Path out;
        try {
            out = Paths.get(outputFile);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
        } catch (InvalidPathException e) {
            System.err.println("Incorrect path to file: " + outputFile);
            return;
        } catch (IOException e) {
            System.err.println("Error with path create: " + e.getMessage());
            return;
        }
        Parser parser = Parser.create();
        File sourceFile = new File(inputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(out)) {
            try {
                CompilationUnitTree cut = parser.parse(sourceFile,
                        System.err::println);
                if (cut != null) {
                    writer.write(cut.accept(new DestructuringAssignmentVisitor(), null).toString());
                } else {
                    System.err.println("cant parse given file");
                }
            } catch (NashornException e) {
                System.err.println("Error while parsing" + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("IOExcetion :" + e.getMessage());
        }
    }

}
 