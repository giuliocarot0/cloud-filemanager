package com.resilientnet.filebridge.controller;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;


@RestController
public class FileController {
    @RolesAllowed({"admin", "user"})
    @RequestMapping(value="/upload", method = RequestMethod.PUT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> fileUpload(@RequestHeader String Authorization,@RequestParam("service") String service,@RequestParam("path") String path, @RequestParam("file") MultipartFile bin) throws  IOException{
        try{
            File file = new File(getBasicDirectory(service)+"/"+ path + "/" +bin.getOriginalFilename());
            if(file.createNewFile()) {
                FileOutputStream fout = new FileOutputStream(file);
                fout.write(bin.getBytes());
                fout.close();
                return ResponseEntity.ok().build();
            }
            else
                return ResponseEntity.notFound().build();
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");

        }
    }

    @RolesAllowed({"admin", "user"})
    @RequestMapping(value="/download", method = RequestMethod.GET)
    public ResponseEntity<Object> downloadFile(@RequestHeader String Authorization,@RequestParam("service") String service, @RequestParam("path") String filePath) {
        try {
            File file = new File(getBasicDirectory(service)+ "/" + filePath);
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
    @RolesAllowed({"admin", "user"})
    @RequestMapping(value="/ls", method = RequestMethod.GET)
    public ResponseEntity<Object> ls(@RequestHeader String Authorization,@RequestParam("service") String service, @RequestParam("path") String path){
        try {
            Map<String, Object> fileMap;
            File file = new File(getBasicDirectory(service) + validatePath(path));

            fileMap = Stream.of(Objects.requireNonNull(file.listFiles())).collect(Collectors.toMap(File::getName, m->{
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
            if(e.getMessage() == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");

            else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unknown error");
        }

    }

    /*delete file*/
    @RolesAllowed({"admin", "user"})
    @RequestMapping(value="/delete", method = RequestMethod.DELETE)
    public ResponseEntity<Object> delete(@RequestHeader String Authorization,@RequestParam("service") String service, @RequestParam("path") String path) {
        /*creating a path list*/
        List<String> paths =  Stream.of(path.split(",")).collect(Collectors.toList());
        String basic_path = getBasicDirectory(service)+"/";

        Map<String, Integer> result = new HashMap<>();
        result.put("deleted", 0);
        result.put("ignored", 0);
        try {
            for(String f: paths){
                    if (!validatePath(f).equals(f))
                        continue;
                    File _f = new File(basic_path + validatePath(f));
                    if (_f.delete())
                        result.replace("deleted", result.get("deleted") + 1);

            }
            result.replace("ignored", (path.split(",").length - result.get("deleted")));
            return ResponseEntity.ok().body(result);

        }catch (Exception e){
          result.replace("ignored", (path.split(",").length - result.get("deleted")));
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }

    }

    /*rename file*/
    @RolesAllowed({"admin", "user"})
    @RequestMapping(value = {"/rename", "/move"}, method = RequestMethod.POST)
    public ResponseEntity<Object> mv(@RequestHeader String Authorization,@RequestParam("service") String service, @RequestParam("src_path") String src, @RequestParam("dst_path") String dst){
        String base_path = getBasicDirectory(service)+"/";

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
    @RolesAllowed({"admin", "user"})
    @RequestMapping(value = {"/copy"}, method = RequestMethod.POST)
    public ResponseEntity<Object> cp(@RequestHeader String Authorization,@RequestParam("service") String service, @RequestParam("src_path") String src, @RequestParam("dst_path") String dst) throws IOException{
        String base_path = getBasicDirectory(service)+"/";
        File src_file = new File(base_path+src);
        File dst_file = new File(base_path+dst);
        dst_file.createNewFile();
        FileOutputStream fout = new FileOutputStream(dst_file);
        fout.write(new FileInputStream(src_file).readAllBytes());
        fout.close();
        return ResponseEntity.ok().body("File is copied successfully");
    }

    /*make dir*/
    @RolesAllowed({"admin", "user"})
    @RequestMapping(value="/createDir", method = RequestMethod.POST)
    public ResponseEntity<Object> mkdir(@RequestHeader String Authorization,@RequestParam("service") String service, @RequestParam("path") String path){
        File _f = new File(getBasicDirectory(service)+"/" + path);
        try {
            if(_f.mkdir())
                return ResponseEntity.ok().body("Created");
            else
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Path not found");
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
    }

    @RolesAllowed({"admin", "user"})
    @GetMapping(value="/user_info")
    public String user(@RequestHeader String Authorization){
       return Subject();
    }

    private String Subject(){
        KeycloakAuthenticationToken authentication = (KeycloakAuthenticationToken) SecurityContextHolder.getContext()
                .getAuthentication();
        KeycloakPrincipal<KeycloakSecurityContext> keycloakPrincipal = (KeycloakPrincipal<KeycloakSecurityContext>) authentication.getPrincipal();

        return keycloakPrincipal.getName() ;
    }
    private String getBasicDirectory(String reqService){
        /*check here if subject is initialized*/
        File _s = new File("/var/usr/share/"+ Subject() + "/" + reqService);
        if(!_s.exists())
            _s.mkdirs();
        return _s.getAbsolutePath();
    }

    private String validatePath(String path){
        /*user cannot access files below the service one*/
        int back = 0, dir=0;
        /*check if path starts with '/' */
        if(path.charAt(0) != '/') return "/";
        List<String> pathElements = Stream.of(path.split("/")).filter(e-> {return !e.equals("");}).collect(Collectors.toList());
        for (String element: pathElements){
            if(element.equals(".."))
                back++;
            else
                dir++;
        }

        if ( back <= dir)
            return path;
        else
            return "/";
    }

}


