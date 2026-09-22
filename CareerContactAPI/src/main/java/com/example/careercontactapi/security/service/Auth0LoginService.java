package com.example.careercontactapi.security.service;

import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.net.URI;
@Slf4j
@Service
@Generated
@RequiredArgsConstructor
public class Auth0LoginService {
    private final String frontendDomain = "http://localhost:3000/";

    public String getFormattedDomain() {
        String url = frontendDomain.replace("https://", "").replace("http://", "")
                .split(":")[0].replace("/", "");

        log.info("Formatted domain: " + url);

        return url;
    }

    public ResponseEntity<Void> getVoidResponseEntity(@AuthenticationPrincipal OidcUser principal) {
        HttpHeaders headers = new HttpHeaders();
        log.info("TEOKEN: " + principal.getIdToken().toString());
        OAuth2AuthenticationToken authentication = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String formattedDomain = getFormattedDomain();

        // TODO .httpOnly should be true, but this was my janky solution
        //  a lot of the stuff here is janky...
        ResponseCookie cookie = ResponseCookie.from("id_token",getToken(authentication, principal.getIdToken()))
                .httpOnly(false)
                .secure(true)
                .domain(formattedDomain)
                .path("/")
                .sameSite("None")
                .build();
        ResponseCookie authCookie = ResponseCookie.from("isAuthenticated", "true")
                .httpOnly(false)
                .secure(true)
                .domain(formattedDomain)
                .path("/")
                .sameSite("None")
                .build();

        String accessPermissions = principal.getClaim("https://careercontact/roles") != null ?
                principal.getClaim("https://careercontact/roles").toString():
                principal.getAuthorities().toString()
                        .replace(",","-").replace(" ","");
        ResponseCookie accessPermissionCookie = ResponseCookie.from("accessPermission", accessPermissions)
                .httpOnly(false)
                .secure(true)
                .domain(formattedDomain)
                .path("/")
                .sameSite("None")
                .build();
        String picture = principal.getClaim("picture");

        ResponseCookie pictureCookie = ResponseCookie.from("picture", picture)
                .httpOnly(false)
                .secure(true)
                .domain(formattedDomain)
                .path("/")
                .sameSite("None")
                .build();
        ResponseCookie userIdCookie = ResponseCookie.from("userId", principal.getSubject())
                .httpOnly(false)
                .secure(true)
                .domain(formattedDomain)
                .path("/")
                .sameSite("None")
                .build();

        log.info("Subject: " + principal.getSubject());
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, accessPermissionCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, pictureCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, userIdCookie.toString());

        if (principal.getClaim("https://careercontact/roles") != null && principal.getClaim("https://careercontact/roles").toString().contains("Admin")) {
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers)
                    .location(URI.create(frontendDomain + "admin")).build();
        }
//        else if (principal.getClaim("https://careercontact.com/roles").toString().contains("Order_Employee")) {
//            return ResponseEntity.status(HttpStatus.FOUND).headers(headers)
//                    .location(URI.create("http://localhost:3000/admin")).build();
//        } else {

            return ResponseEntity.status(HttpStatus.FOUND).headers(headers)
                    .location(URI.create(frontendDomain)).build();
        //}
    }

    public String getToken(OAuth2AuthenticationToken oAuth2AuthenticationToken, @AuthenticationPrincipal(expression = "idToken") OidcIdToken idToken) {
        return idToken.getTokenValue();
    }
}
