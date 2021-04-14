package com.resilientnet.filebridge.controller;
import java.io.*;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {

    @RequestMapping(value="/upload", method = RequestMethod.PUT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String fileUpload(@RequestParam("file") MultipartFile file) throws  IOException{
        File convertFile = new File(""+file.getOriginalFilename());
        convertFile.createNewFile();
        FileOutputStream fout = new FileOutputStream(convertFile);
        fout.write(file.getBytes());
        fout.close();
        return "File is upload successfully";
    }

    @RequestMapping(value="/download", method = RequestMethod.GET)
    public ResponseEntity<Object> downloadFile(@RequestParam("path") String filePath) throws IOException{
        File file = new File("C:\\Users\\giuli\\Desktop\\filebridge\\" + filePath);
        InputStreamResource res = new InputStreamResource(new FileInputStream(file));
        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(
                MediaType.parseMediaType("application/binary")).body(res);
    }
    /*list file*/
    @RequestMapping(value="/ls", method = RequestMethod.GET)
    public ResponseEntity<Object> ls(@RequestParam("path") String path) throws IOException{
        Map<String, Map<String, String>> fileMap= new HashMap<>();
        try {
            File file = new File("C:\\Users\\giuli\\Desktop\\filebridge\\" + path);
            Arrays.stream(file.list()).forEach(f -> {
                File _file = new File(f);
                Map<String, String> _fileMeta = new HashMap<>();
                if(_file.isFile()){
                    _fileMeta.put("last_mod",(new Date(_file.lastModified()).toString()));
                    _fileMeta.put("size",_file.length() + " B");
                    _fileMeta.put("type", "file");
                    _fileMeta.put("permissions",  (_file.canWrite() ? "r":"-") +( _file.canRead()? "w":"-") + (_file.canExecute() ? "x":"-") );
                    fileMap.put(f, _fileMeta);
                }
            });
            return ResponseEntity.ok().body(fileMap);
        }
        catch(Exception e){
            return ResponseEntity.notFound().build();
        }

    }

    /*delete file*/
    /*rename file*/
    /*move file*/
    /*copy file*/
    /*make dir*/
    /*remove dir*/
}

