package ch.hslu.assmbler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Assembler {

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("No input file provided");
        }
        Path input = Path.of(args[0]);
        Path parentDir = input.getParent();
        File output;

        try {
            output = new File(parentDir.resolve(FilenameUtils.getBaseName(input.toString()) + ".hack").toUri());
            output.createNewFile();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create output file", e);
        }


        try (Parser parser = new Parser(Path.of(args[0]));
             BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {

            for (Instruction i : parser) {
                writer.write(i.getBinaryCode());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            output.delete();
            throw new IllegalStateException("Error while assembling file " + input, e);
        }
    }
}
