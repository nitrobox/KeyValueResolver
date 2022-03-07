package com.nitrobox.keyvalueresolver;

import java.io.FileReader;
import java.io.IOException;

public class TestMain {

    public static void main(String[] args) throws IOException {
        final FileReader fileReader = new FileReader("myFile.txt");
        System.out.println(fileReader.read());
    }
}
