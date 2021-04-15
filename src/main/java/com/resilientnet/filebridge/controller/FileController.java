package com.resilientnet.filebridge.controller;
import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

            fileMap = Stream.of(file.listFiles()).collect(Collectors.toMap(File::getName, m->{
                Map<String, String> _fileMeta = new HashMap<>();
                _fileMeta.put("last_mod",(new Date(m.lastModified()).toString()));
                _fileMeta.put("size",m.length() + " B");
                _fileMeta.put("type", m.isFile() ? "file":"directory");
                _fileMeta.put("permissions",  (m.canWrite() ? "r":"-") +( m.canRead()? "w":"-") + (m.canExecute() ? "x":"-") );

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
        /*creating a path list*/
        List<String> paths =  Stream.of(path.split(",")).collect(Collectors.toList());
        String basic_path = "C:\\Users\\giuli\\Desktop\\filebridge\\filesystem\\";

        Map<String, Integer> result = new HashMap<>();
        result.put("deleted", 0);
        result.put("ignored", 0);
        try {
            paths.forEach(f->{
                    File _f = new File(basic_path + f);
                    if (_f.delete())
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
    @RequestMapping(value = {"/rename", "/move"}, method = RequestMethod.POST)
    public ResponseEntity<Object> mv( @RequestParam("src_path") String src, @RequestParam("dst_path") String dst){
        String base_path = "C:\\Users\\giuli\\Desktop\\filebridge\\filesystem\\";

        try {
            File src_f = new File(base_path+src);
            File dst_f = new File(base_path+dst);
            if (!src_f.renameTo(dst_f))
                throw new Exception("CANT_RENAME");
            return ResponseEntity.ok().body("File moved");
        }catch (Exception e){
            if(e.getMessage().equals("CANT_RENAME"))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("<h1>Can't rename or move the current file</h1>");
            else
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("<h1>Can't found requested file </h1>");
        }
    }
    /*copy file*/
    @RequestMapping(value = {"/copy"}, method = RequestMethod.POST)
    public ResponseEntity<Object> cp(@RequestParam("src_path") String src, @RequestParam("dst_path") String dst) throws IOException{
        File src_file = new File("filesystem/"+src);
        File dst_file = new File("filesystem/"+dst);
        dst_file.createNewFile();
        FileOutputStream fout = new FileOutputStream(dst_file);
        fout.write(new FileInputStream(src_file).readAllBytes());
        fout.close();
        return ResponseEntity.ok().body("File is copied successfully");
    }
    /*make dir*/
    /*remove dir*/
}

