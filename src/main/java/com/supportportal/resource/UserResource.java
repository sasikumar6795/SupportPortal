package com.supportportal.resource;

import com.supportportal.domain.User;
import com.supportportal.domain.UserPrincipal;
import com.supportportal.exception.ExceptionHandling;
import com.supportportal.service.UserService;
import com.supportportal.utility.JWTTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import static com.supportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;

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
        return new ResponseEntity<>(registerNewUser, HttpStatus.OK);
    }
    
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user)
    {
        authenticateLoggingInUser(user.getUserName(),user.getPassword());
        User loginUser = userService.findByUserName(user.getUserName());
        UserPrincipal userPrincipal =  new UserPrincipal(loginUser);
        HttpHeaders jwtHeaders = getJWTHeaders(userPrincipal);
        return new ResponseEntity<>(loginUser,jwtHeaders, HttpStatus.OK);
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
