package com.supportportal.utility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.supportportal.constant.SecurityConstant;
import com.supportportal.domain.UserPrincipal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.supportportal.constant.SecurityConstant.*;

@Component
public class JWTTokenProvider {
    //usually you will store in secure server and have it in a property file
    @Value("${jwt.secret}")
    private String secret ="";

    //how to generate jwt token
    // generateClaimsFromUser -> getAuthorities -> getClaimsFromToken -> getJWTVerifier ->
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

    public Authentication getAuthentication(String userName, List<GrantedAuthority> authorities, HttpServletRequest httpServletRequest)
    {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                =new UsernamePasswordAuthenticationToken(userName,null,authorities);
        //in order to tell spring about the user
        usernamePasswordAuthenticationToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
        return usernamePasswordAuthenticationToken;
    }

    public boolean isTokenValid(String userName, String token)
    {
        JWTVerifier verifier =  getJWTVerifier();
        return StringUtils.isNotEmpty(userName) && !isTokenExpired(verifier,token);
    }

    public String getSubject(String token){
        JWTVerifier verifier =  getJWTVerifier();
        return verifier.verify(token).getSubject();
    }

    private boolean isTokenExpired(JWTVerifier verifier, String token) {
        Date expiration = verifier.verify(token).getExpiresAt();
        return expiration.before(new Date());
    }

    private String[] getClaimsFromToken(String token) {
        JWTVerifier jwtVerifier=getJWTVerifier();
        return jwtVerifier.verify(token).getClaim(AUTHORITIES).asArray(String.class);
    }

    private JWTVerifier getJWTVerifier() {
        JWTVerifier jwtVerifier;
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            jwtVerifier=JWT.require(algorithm).withIssuer(GET_ARRAYS_LLC).build();
        }catch (JWTVerificationException jwtVerificationException){
            throw new JWTVerificationException(TOKEN_CANNOT_BE_VERIFIED);
        }
        return jwtVerifier;
    }

    private  String[] generateClaimsFromUser(UserPrincipal userPrincipal)
    {
        List<String> authorities = new ArrayList<>();
        return userPrincipal.getAuthorities().stream()
                .map(grantedAuthority -> authorities.add(grantedAuthority.getAuthority()))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }
}
