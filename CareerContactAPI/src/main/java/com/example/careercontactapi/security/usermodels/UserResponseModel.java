package com.example.careercontactapi.security.usermodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import java.util.List;

@EqualsAndHashCode(callSuper=false)
@Data
@AllArgsConstructor
@Generated
public class UserResponseModel {
    private String username;
    private String email;
    private List<String> roles;
}
