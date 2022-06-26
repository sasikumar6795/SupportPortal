package com.supportportal.listener;

import com.supportportal.service.LoginAttempt;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureListener {

    private LoginAttempt loginAttempt;

    public AuthenticationFailureListener(LoginAttempt loginAttempt) {
        this.loginAttempt = loginAttempt;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event)  {
        Object principal = event.getAuthentication().getPrincipal();
        if(principal instanceof String)
        {
            String userName = (String) event.getAuthentication().getPrincipal();
            loginAttempt.addUserToLoginAttempts(userName);
        }
    }
}
