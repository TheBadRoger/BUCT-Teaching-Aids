package com.buctta.api.service;

import java.io.FileNotFoundException;
import java.io.*;

public interface FileExtract {
    String ExtractDocxFile(String path) throws IOException;
    String ExtractPdfFile(String path) throws IOException;
}
