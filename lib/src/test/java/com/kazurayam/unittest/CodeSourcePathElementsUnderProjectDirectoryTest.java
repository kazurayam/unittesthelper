package com.kazurayam.unittest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeSourcePathElementsUnderProjectDirectoryTest {

    @Test
    public void testConstructor_single_string() {
        CodeSourcePathElementsUnderProjectDirectory cspeupd =
                new CodeSourcePathElementsUnderProjectDirectory("build/classes/java/test");
        assertThat(cspeupd.toString()).isEqualTo("build/classes/java/test/");
    }

    @Test
    public void testConstructor_multiple_strings() {
        CodeSourcePathElementsUnderProjectDirectory cspeupd =
                new CodeSourcePathElementsUnderProjectDirectory(
                        "build", "classes", "java", "test");
        assertThat(cspeupd.toString()).isEqualTo("build/classes/java/test/");
    }

    @Test
    public void testConstructor_Path() {
        CodeSourcePathElementsUnderProjectDirectory cspeupd =
                new CodeSourcePathElementsUnderProjectDirectory(
                        Paths.get("build/classes/java/test"));
        assertThat(cspeupd.toString()).isEqualTo("build/classes/java/test/");
    }

    @Test
    public void testConstructor_ListOfStrings() {
        CodeSourcePathElementsUnderProjectDirectory cspeupd =
                new CodeSourcePathElementsUnderProjectDirectory(
                        Arrays.asList("build", "classes", "java", "test"));
        assertThat(cspeupd.toString()).isEqualTo("build/classes/java/test/");
    }
}
