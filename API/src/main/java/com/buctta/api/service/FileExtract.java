package com.buctta.api.service;

import com.buctta.api.utils.CallBackContainer;
import java.io.*;

public interface FileExtract {
    String ExtractDocxFile(String path) throws IOException;
    String ExtractPdfFile(String path) throws IOException;
    CallBackContainer<String> DocumentExtractor(String path) throws IOException;
}
