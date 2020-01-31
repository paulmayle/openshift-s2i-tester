package edu.ucla.mednet.iss.automation;


import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

     String storeFile(MultipartFile file);
     Resource loadFileAsResource(String fileName) ;
}
