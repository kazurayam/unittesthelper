package com.example;

import com.kazurayam.unittest.CodeSourcePathElementsUnderProjectDirectory;
import com.kazurayam.unittest.TestOutputOrganizer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public class App {

    private static TestOutputOrganizer too =
            new TestOutputOrganizer.Builder(App.class)
                    .addCodeSourcePathElementsUnderProjectDirectory(
                            new CodeSourcePathElementsUnderProjectDirectory(
                                    "build", "classes", "java", "main"))
                    .subOutputDirectory(App.class)
                    .build();

    public Path sayHelloTo(String name) throws IOException {
        Path outputDirectory = too.cleanClassOutputDirectory();
        Path output = outputDirectory.resolve("greeting.txt");
        Files.writeString(output, "Hello, " + name + "!");
        return output;
    }
}
