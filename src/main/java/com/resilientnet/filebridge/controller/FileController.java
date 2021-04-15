package com.resilientnet.filebridge.controller;
import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;


import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    public String fileUpload(@RequestParam("file") MultipartFile bin) throws  IOException{
        File file = new File("filesystem/"+bin.getOriginalFilename());
        file.createNewFile();
        FileOutputStream fout = new FileOutputStream(file);
        fout.write(bin.getBytes());
        fout.close();
        return "File is upload successfully";
    }

    @RequestMapping(value="/download", method = RequestMethod.GET)
    public ResponseEntity<Object> downloadFile(@RequestParam("path") String filePath) throws IOException {
        try {
            File file = new File("C:\\Users\\giuli\\Desktop\\filebridge\\filesystem" + filePath);
            InputStreamResource res = new InputStreamResource(new FileInputStream(file));
            HttpHeaders headers = new HttpHeaders();

            headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(
                    MediaType.parseMediaType("application/binary")).body(res);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    /*list file*/
    @RequestMapping(value="/ls", method = RequestMethod.GET)
    public ResponseEntity<Object> ls(@RequestParam("path") String path) throws IOException{
        try {
            Map<String, Object> fileMap;
            File file = new File("C:\\Users\\giuli\\Desktop\\filebridge\\filesystem\\" + path);

            fileMap = Arrays.stream(file.list()).collect(Collectors.toMap(f->f, m->{
                File _file = new File(m);
                Map<String, String> _fileMeta = new HashMap<>();
                if(_file.isFile()){
                    _fileMeta.put("last_mod",(new Date(_file.lastModified()).toString()));
                    _fileMeta.put("size",_file.length() + " B");
                    _fileMeta.put("type", "file");
                    _fileMeta.put("permissions",  (_file.canWrite() ? "r":"-") +( _file.canRead()? "w":"-") + (_file.canExecute() ? "x":"-") );
                }
                return _fileMeta;
            }));
            return ResponseEntity.ok().body(fileMap);
        }
        catch(Exception e){
            return ResponseEntity.notFound().build();
        }

    }

    /*delete file*/
    @RequestMapping(value="/delete", method = RequestMethod.DELETE)
    public ResponseEntity<Object> delete(@RequestParam("path") String path) throws Exception{
        path = "C:\\Users\\giuli\\Desktop\\filebridge\\filesystem\\" + path;
        Map<String, Integer> result = new HashMap<>();
        result.put("deleted", 0);
        result.put("ignored", 0);
        try {
            Arrays.stream(path.split(",")).forEach(f->{
                File _f = new File(f);

                if(_f.delete())
                    result.replace("deleted", result.get("deleted") + 1);
                else
                    result.replace("ignored", result.get("ignored") + 1);
            });
            return ResponseEntity.ok().body(result);

        }catch (Exception e){
          result.replace("ignored", (path.split(",").length - result.get("deleted")));
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

    }

    /*rename file*/
    /*move file*/
    /*copy file*/
    /*make dir*/
    /*remove dir*/
}

