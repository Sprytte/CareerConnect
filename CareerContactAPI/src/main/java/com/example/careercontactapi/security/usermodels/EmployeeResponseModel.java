package com.example.careercontactapi.security.usermodels;

import lombok.*;

@EqualsAndHashCode(callSuper=false)
@Data
@AllArgsConstructor
@Generated
@Builder
@NoArgsConstructor
public class EmployeeResponseModel {
    private String user_id;
    private String email;
    private String picture;
    private String name;
}
