package com.kazurayam.unittesthelperdemo;

import com.example.App;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExampleA5Test {

    @Test
    public void test_com_example_App() throws IOException {
        App app = new App();
        Path output = app.sayHelloTo("William Tell");
        assertTrue(Files.exists(output));
    }
}
