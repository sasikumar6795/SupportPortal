package com.supportportal.resource;

import com.supportportal.constant.UserImplementationConstant;
import com.supportportal.domain.HttpResponse;
import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.exception.ExceptionHandling;
import com.supportportal.service.UserService;
import com.supportportal.utility.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.supportportal.constant.FileConstant.*;
import static com.supportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@RestController
@RequestMapping(path = {"/","/user"})
public class UserResource extends ExceptionHandling {

    private UserService userService;
    private AuthenticationManager authenticationManager;
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(UserService userService, AuthenticationManager authenticationManager, JWTTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/home")
    public String getUser()
    {
        return "Hey Sasikumar";
    }

    @PostMapping("/register")
    public ResponseEntity<User> register (@RequestBody User user)
    {
        User registerNewUser = userService.register(user.getFirstName(), user.getLastName(), user.getUserName(), user.getEmail());
        return new ResponseEntity<>(registerNewUser, OK);
    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(@RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("userName") String userName,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("nonLocked") String nonLocked,
                                           @RequestParam(value="profileImage", required = false) MultipartFile profileImage){
        Optional<User> newUser = userService.addNewUser(firstName, lastName, userName, email, role, Boolean.parseBoolean(isActive), Boolean.parseBoolean(nonLocked), profileImage);
        return new ResponseEntity(newUser, OK);
    }

    @PostMapping("/update")
    public ResponseEntity<User> updateNewUser(@RequestParam("currentUserName") String currentUserName,
                                           @RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("userName") String userName,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam("nonLocked") String nonLocked,
                                           @RequestParam(value="profileImage", required = false) MultipartFile profileImage){
        Optional<User> updateduser = userService.updateUser(currentUserName,firstName, lastName, userName, email, role, Boolean.parseBoolean(isActive), Boolean.parseBoolean(nonLocked), profileImage);
        return new ResponseEntity(updateduser, OK);
    }


    @GetMapping("/find/{userName}")
    public ResponseEntity<User> getUser(@PathVariable("userName") String userName)
    {
        User user = userService.findByUserName(userName);
        return new ResponseEntity<>(user, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> userList = userService.getUsers();
        return new ResponseEntity<>(userList, OK);
    }

    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> getAllUsers(@PathVariable("email") String email) {
        userService.resetPassword(email);
        return response(OK, UserImplementationConstant.AN_EMAIL_WITH_NEW_PASSWORD_SENT_TO + email);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("id") long id)
    {
        userService.deleteUser(id);
        return response(NO_CONTENT, UserImplementationConstant.USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> updateProfileImage(@RequestParam("userName") String userName, @RequestParam(value="profileImage") MultipartFile profileImage){
        Optional<User> updatedUserWithProfileImage = userService.updateProfileImage(userName,profileImage);
        return new ResponseEntity(updatedUserWithProfileImage, OK);
    }

    @GetMapping(value = "/image/{userName}/{fileName}", produces = {IMAGE_JPEG_VALUE})
    public byte[] getProfileImage(@PathVariable("userName") String userName, @PathVariable("fileName") String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(USER_FOLDER+userName+FORWARD_SLASH+fileName));
    }

    @GetMapping(value = "/image/profile/{userName}", produces = {IMAGE_JPEG_VALUE})
    public byte[] getTempProfileImage(@PathVariable("userName") String userName) throws IOException {
        URL url = new URL(TEMP_PROFILE_IMAGE_BASE_URL+userName);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        InputStream inputStream = url.openStream();
        int bytesRead;
        byte[] chunk = new byte[1024];
        while((bytesRead=inputStream.read(chunk)) > 0) {
            byteArrayOutputStream.write(chunk,0,bytesRead);
        }

        return byteArrayOutputStream.toByteArray();
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user)
    {
        authenticateLoggingInUser(user.getUserName(),user.getPassword());
        User loginUser = userService.findByUserName(user.getUserName());
        UserPrincipal userPrincipal =  new UserPrincipal(loginUser);
        HttpHeaders jwtHeaders = getJWTHeaders(userPrincipal);
        return new ResponseEntity<>(loginUser,jwtHeaders, OK);
    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        HttpResponse body = new HttpResponse(httpStatus.value(), httpStatus, httpStatus.getReasonPhrase(), message.toUpperCase());
        return new ResponseEntity<>(body,httpStatus);
    }

    private HttpHeaders getJWTHeaders(UserPrincipal userPrincipal) {
        HttpHeaders headers =  new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER,jwtTokenProvider.generateJWTToken(userPrincipal));
        return headers;
    }

    private void authenticateLoggingInUser(String userName, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName,password));
    }


}
