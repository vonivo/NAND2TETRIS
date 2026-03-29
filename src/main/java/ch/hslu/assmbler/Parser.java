package ch.hslu.assmbler;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class Parser implements Iterable<Instruction>, Closeable {
    private static final Pattern LINE_REGEX = Pattern.compile("^\\s*(\\(|//|$)");
    private static final Pattern INLINE_COMMNET_REGEX = Pattern.compile("\\/\\/.*$");

    private final Map<String, Integer> symbolTable = new HashMap<>();
    private final Path inputFile;
    private final BufferedReader bufferedReader;

    public Parser(Path inputFile) {
        this.inputFile = inputFile;

        if (inputFile == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        if (!FilenameUtils.getExtension(inputFile.toString()).equals("asm")) {
            throw new IllegalArgumentException("Input " + inputFile + " is no asm file");
        }

        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("File " + inputFile + " does not exist.");
        }

        try {
            bufferedReader = Files.newBufferedReader(inputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open file " + inputFile, e);
        }

        buildInitialSymbolTable();
        try {
            buildSymbolTableWithLabels();
            buildSymbolTableWithVariables();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot build symbol table");
        }
    }

    private void buildSymbolTableWithLabels() throws IOException {
        try (var reader = new BufferedReader(Files.newBufferedReader(inputFile))) {
            String line = reader.readLine();
            int lineNumber = 0;
            int variableCount = 0;
            while (line != null) {
                line = INLINE_COMMNET_REGEX.matcher(line).replaceAll("").trim();


                if (!line.isBlank()) {
                    if ('(' == line.charAt(0)) {
                        String label = line.substring(1, line.length() - 1);
                        symbolTable.put(label, lineNumber);
                    } else if (!LINE_REGEX.matcher(line).find()) {
                        lineNumber++;
                    }
                }
                line = reader.readLine();
            }
        }
    }

    private void buildSymbolTableWithVariables() throws IOException {
        try (var reader = new BufferedReader(Files.newBufferedReader(inputFile))) {
            String line = reader.readLine();
            int variableCount = 0;

            while (line != null) {
                line = INLINE_COMMNET_REGEX.matcher(line).replaceAll("").trim();

                if (!line.isBlank()) {
                    if ('@' == line.charAt(0)
                            && !Character.isDigit(line.charAt(1))
                            && !symbolTable.containsKey(line.substring(1))) {
                        symbolTable.putIfAbsent(line.substring(1), 16 + variableCount++);

                    }
                }
                line = reader.readLine();
            }
        }
    }

    private void buildInitialSymbolTable() {
        symbolTable.put("R0", 0);
        symbolTable.put("R1", 1);
        symbolTable.put("R2", 2);
        symbolTable.put("R3", 3);
        symbolTable.put("R4", 4);
        symbolTable.put("R5", 5);
        symbolTable.put("R6", 6);
        symbolTable.put("R7", 7);
        symbolTable.put("R8", 8);
        symbolTable.put("R9", 9);
        symbolTable.put("R10", 10);
        symbolTable.put("R11", 11);
        symbolTable.put("R12", 12);
        symbolTable.put("R13", 13);
        symbolTable.put("R14", 14);
        symbolTable.put("R15", 15);
        symbolTable.put("SP", 0);
        symbolTable.put("LCL", 1);
        symbolTable.put("ARG", 2);
        symbolTable.put("THIS", 3);
        symbolTable.put("THAT", 4);
        symbolTable.put("SCREEN", 16384);
        symbolTable.put("KB", 24576);
    }

    @Override
    public Iterator<Instruction> iterator() {
        return new ParserIterator();
    }

    @Override
    public void close() throws IOException {
        if (bufferedReader != null) {
            bufferedReader.close();
        }
    }


    private class ParserIterator implements Iterator<Instruction> {
        private String nextLine;

        public ParserIterator() {
            advance();
        }

        private void advance() {
            try {
                do {
                    nextLine = bufferedReader.readLine();
                } while (nextLine != null && LINE_REGEX.matcher(nextLine).find());
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read line", e);
            }
        }

        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        @Override
        public Instruction next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            String currentLine = nextLine;
            currentLine = INLINE_COMMNET_REGEX.matcher(currentLine).replaceAll("").trim();
            advance(); // prepare next

            if ('@' == currentLine.charAt(0)) {
                return new AInstruction(currentLine);
            }

            return new CInstrction(currentLine);
        }
    }

    private class AInstruction implements Instruction {
        private final String assembly;

        public AInstruction(String assembly) {
            this.assembly = assembly;
        }

        @Override
        public String getBinaryCode() {
            var symbolOrValue = assembly.substring(1);
            int value = 0;
            if (symbolTable.containsKey(symbolOrValue)) {
                value = symbolTable.get(symbolOrValue);
            } else {
                value = Integer.parseInt(symbolOrValue);
            }
            return String.format("%16s", Integer.toBinaryString(value))
                    .replace(' ', '0');
        }
    }

    private class CInstrction implements Instruction {
        private static final Map<Character, Integer> DESTINATIONS = Map.of('M', 1, 'D', 2, 'A', 4);
        private static final Map<String, Integer> JUMPS = Map.of(
                "JGT", 1,
                "JEQ", 2,
                "JGE", 3,
                "JLT", 4,
                "JNE", 5,
                "JLE", 6,
                "JMP", 7
        );
        private static final Map<String, String> COMP = new HashMap<>();

        private final String assembly;

        public CInstrction(String assembly) {
            this.assembly = assembly.trim();

            COMP.put("0", "101010");
            COMP.put("1", "111111");
            COMP.put("-1", "111010");
            COMP.put("D", "001100");
            COMP.put("A", "110000");
            COMP.put("!D", "001101");
            COMP.put("!A", "110001");
            COMP.put("-D", "001111");
            COMP.put("-A", "110011");
            COMP.put("D+1", "011111");
            COMP.put("A+1", "110111");
            COMP.put("D-1", "001110");
            COMP.put("A-1", "110010");
            COMP.put("D+A", "000010");
            COMP.put("D-A", "010011");
            COMP.put("A-D", "000111");
            COMP.put("D&A", "000000");
            COMP.put("D|A", "010101");
        }

        private String getDest() {
            int value = 0;
            if (assembly.contains("=")) {
                var desString = assembly.substring(0, assembly.indexOf('='));
                for (char c : desString.toCharArray()) {
                    value += DESTINATIONS.get(c);
                }
            }
            return String.format("%3s", Integer.toBinaryString(value))
                    .replace(' ', '0');
        }

        private String getJump() {
            int value = 0;
            if (assembly.contains(";")) {
                var jump = assembly.substring(assembly.indexOf(';') + 1);
                value = JUMPS.get(jump);
            }
            return String.format("%3s", Integer.toBinaryString(value))
                    .replace(' ', '0');
        }

        private String getComp() {
            int startIndex = assembly.contains("=") ? assembly.indexOf('=') + 1 : 0;
            int endIndex = assembly.contains(";") ? assembly.indexOf(';') : assembly.length();

            String comp = assembly.substring(startIndex, endIndex);

            int aBit = comp.contains("M") ? 1 : 0;
            comp = comp.replace("M", "A");

            return aBit + COMP.get(comp);
        }

        @Override
        public String getBinaryCode() {
            return "111" + getComp() + getDest() + getJump();
        }
    }
}
