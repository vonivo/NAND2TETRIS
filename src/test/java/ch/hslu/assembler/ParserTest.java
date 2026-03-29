package ch.hslu.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import ch.hslu.assmbler.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {
    private static final String ASSEMBLY_FILE_NAME = "assembly.asm";

    @TempDir
    private Path tempdir;


    @Test
    void testConstructor() throws IOException {
        // arrange
        createAsmFile("");

        // act
        var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME));
    }

    @Test
    void testConstructor_nonExistingFile_shouldThrowError() {
        // act & assert
         assertThrows(IllegalArgumentException.class, () -> new Parser(tempdir.resolve("invalid.asm")));
    }

    @Test
    void testConstructor_null_shouldThrowError() {
        // act & assert
        assertThrows(IllegalArgumentException.class, () -> new Parser(null));
    }

    @Test
    void testConstructor_wrongFileExtension_shouldThrowError() throws IOException {
        // arrange
        var file = new File(tempdir.resolve("input.wrong").toUri());
        file.createNewFile();
        // act & assert
        assertThrows(IllegalArgumentException.class, () -> new Parser(tempdir.resolve("input.wrong")));
    }

    @ParameterizedTest
    @CsvSource({
            "@R0, 0000000000000000",
            "@R1, 0000000000000001",
            "@R2, 0000000000000010",
            "@R3, 0000000000000011",
            "@R4, 0000000000000100",
            "@R5, 0000000000000101",
            "@R6, 0000000000000110",
            "@R7, 0000000000000111",
            "@R8, 0000000000001000",
            "@R9, 0000000000001001",
            "@R10, 0000000000001010",
            "@R11, 0000000000001011",
            "@R12, 0000000000001100",
            "@R13, 0000000000001101",
            "@R14, 0000000000001110",
            "@R15, 0000000000001111",
            "@SP, 0000000000000000",
            "@LCL, 0000000000000001",
            "@ARG, 0000000000000010",
            "@THIS, 0000000000000011",
            "@THAT, 0000000000000100",
            "@SCREEN, 0100000000000000",
            "@KB, 0110000000000000",
    })
    void testReservedSymbols(String symbol, String assembledInstruction) throws IOException {
        // arrange
        createAsmFile(symbol);

        List<String> assembled = new ArrayList<>();
        try(var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))){

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled).hasSize(1);
        assertThat(assembled.getFirst()).isEqualTo(assembledInstruction);
    }

    @Test
    void testLabels_DefinitionFirst() throws IOException {
        // arrange
        String assembly = "@R0\n(LOOP)\n@LOOP";
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try(var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))){

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        // assert
        assertThat(assembled).hasSize(2);
        assertThat(assembled.getLast()).isEqualTo("0000000000000001");
    }

    @Test
    void testLabels_UsageFirst() throws IOException {
        // arrange
        String assembly = "@LOOP\n@R0\n@R1\n(LOOP)";
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try(var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))){

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        // assert
        assertThat(assembled).hasSize(3);
        assertThat(assembled.getFirst()).isEqualTo("0000000000000011");
    }

    @Test
    void testVariables() throws IOException {
        // arrange
        String assembly = "@i\n@R0\n@R1";
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try (var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))) {

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled.getFirst()).isEqualTo("0000000000010000");
    }

    @Test
    void testVariables_withTowVariables() throws IOException {
        // arrange
        String assembly = "@i\n@a\n@R1";
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try (var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))) {

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled.getFirst()).isEqualTo("0000000000010000");
        assertThat(assembled.get(1)).isEqualTo("0000000000010001");
    }

    @Test
    void testVariables_shouldUseTheSameBinaryCode() throws IOException {
        // arrange
        String assembly = "@i\n@R1\n@i";
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try (var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))) {

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled.getFirst()).isEqualTo("0000000000010000");
        assertThat(assembled.getLast()).isEqualTo("0000000000010000");
    }

    @Test
    void testConstantValue() throws IOException {
        // arrange
        String assembly = "@i\n@R1\n@100";
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try (var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))) {

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled.getLast()).isEqualTo("0000000001100100");
    }

    @ParameterizedTest
    @CsvSource({
            "M=0, 1110101010001000",
            "A=0, 1110101010100000",
            "D=0, 1110101010010000",
            "MD=0, 1110101010011000",
            "AM=0, 1110101010101000",
            "AD=0, 1110101010110000",
            "DMA=0, 1110101010111000",
            "0;JMP, 1110101010000111",
            "0;JGT, 1110101010000001",
            "0;JEQ, 1110101010000010",
            "0;JGE, 1110101010000011",
            "0;JLT, 1110101010000100",
            "0;JNE, 1110101010000101",
            "0;JLE, 1110101010000110",
            "D=1, 1110111111010000",
            "D=-1, 1110111010010000",
            "D=D, 1110001100010000",
            "D=A, 1110110000010000",
            "D=!D, 1110001101010000",
            "D=!A, 1110110001010000",
            "D=-D, 1110001111010000",
            "D=-A, 1110110011010000",
            "D=D+1, 1110011111010000",
            "D=A+1, 1110110111010000",
            "D=A-1, 1110110010010000",
            "D=D-1, 1110001110010000",
            "D=D+A, 1110000010010000",
            "D=D-A, 1110010011010000",
            "D=A-D, 1110000111010000",
            "D=D&A, 1110000000010000",
            "D=D|A, 1110010101010000",
    })
    void testCInstruction(String assembly, String code) throws IOException {
        // arrange
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try (var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))) {

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled.getLast()).isEqualTo(code);
    }

    @ParameterizedTest
    @CsvSource({
            "@a // the value A, 0000000000010000",
            "DM=-1 // SET data and memroy to -1, 1110111010011000"
    })
    void testIgnoreComment(String assembly, String code) throws IOException {
        // arrange
        createAsmFile(assembly);

        List<String> assembled = new ArrayList<>();
        try (var parser = new Parser(tempdir.resolve(ASSEMBLY_FILE_NAME))) {

            // act
            for (var ins : parser) {
                assembled.add(ins.getBinaryCode());
            }
        }

        assertThat(assembled.getLast()).isEqualTo(code);
    }


    private void createAsmFile(String content) throws IOException {
        var file = new File(tempdir.resolve(ASSEMBLY_FILE_NAME).toUri());
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }
}