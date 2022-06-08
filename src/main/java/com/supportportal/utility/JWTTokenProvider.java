package com.supportportal.utility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.supportportal.constant.SecurityConstant;
import com.supportportal.domain.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.supportportal.constant.SecurityConstant.*;

public class JWTTokenProvider {
    //usually you will store in secure server and have it in a property file
    @Value("${jwt.secret}")
    private String secret ="";

    public String generateJWTToken(UserPrincipal userPrincipal)
    {
        String[] claims =  generateClaimsFromUser(userPrincipal);
        return JWT.create()
                .withIssuer(GET_ARRAYS_LLC)
                .withAudience(GET_ARRAYS_ADMINISTRATION)
                .withIssuedAt(new Date())
                .withSubject(userPrincipal.getUsername())// user id
                .withArrayClaim(AUTHORITIES, claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(secret.getBytes()));
    }

    public List<GrantedAuthority> getAuthorities(String token)
    {
        String[] claims =  getClaimsFromToken(token);
        return Arrays.stream(claims).map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public String[] getClaimsFromToken(String token) {
        JWTVerifier jwtVerifier=getJWTVerifier();
        return jwtVerifier.verify(token).getClaim(AUTHORITIES).asArray(String.class);
    }

    public JWTVerifier getJWTVerifier() {
        JWTVerifier jwtVerifier;
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            jwtVerifier=JWT.require(algorithm).withIssuer(GET_ARRAYS_LLC).build();
        }catch (JWTVerificationException jwtVerificationException){
            throw new JWTVerificationException(TOKEN_CANNOT_BE_VERIFIED);
        }
        return jwtVerifier;
    }

    public  String[] generateClaimsFromUser(UserPrincipal userPrincipal)
    {
        List<String> authorities = new ArrayList<>();
        return userPrincipal.getAuthorities().stream()
                .map(grantedAuthority -> authorities.add(grantedAuthority.getAuthority()))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }
}
