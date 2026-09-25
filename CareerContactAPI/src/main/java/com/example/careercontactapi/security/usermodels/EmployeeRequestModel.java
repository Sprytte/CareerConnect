package com.example.careercontactapi.security.usermodels;

import lombok.*;

@EqualsAndHashCode(callSuper=false)
@Data
@AllArgsConstructor
@Generated
@Builder
public class EmployeeRequestModel {
    private String email;
    private String firstName;
    private String lastName;
    private String password;
}
