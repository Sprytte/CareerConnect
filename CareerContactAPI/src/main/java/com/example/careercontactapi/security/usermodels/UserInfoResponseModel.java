package com.example.careercontactapi.security.usermodels;

import lombok.*;
import java.util.HashMap;
@EqualsAndHashCode(callSuper=false)
@Data
@AllArgsConstructor
@Builder
@Generated
public class UserInfoResponseModel {
    private String userId;
    private String email;
    private String name;
    private String picture;
    private HashMap<String, String> user_metadata;
}
