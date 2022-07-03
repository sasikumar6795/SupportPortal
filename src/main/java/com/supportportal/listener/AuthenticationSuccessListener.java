package com.supportportal.listener;

import com.supportportal.domain.User;
import com.supportportal.service.LoginAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener {

    private LoginAttempt loginAttempt;

    @Autowired
    public AuthenticationSuccessListener(LoginAttempt loginAttempt) {
        this.loginAttempt = loginAttempt;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if(principal instanceof User)
        {
            User user = (User) event.getAuthentication().getPrincipal();
            loginAttempt.addUserToLoginAttempts(user.getUserName());
        }
    }
}
