package com.seaurl.platform.users.utils;

import java.util.regex.Pattern;

public class InputHelper {
    public static boolean checkUsername(String username){
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9]{4,16}$");
        return pattern.matcher(username).matches();
    }

    public static boolean checkEmail(String email){
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        return pattern.matcher(email).matches();
    }

    public static boolean checkPassword(String password){
        Pattern pattern = Pattern.compile("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_!@#$%^&*`~()-+=]+$)(?![a-z0-9]+$)(?![a-z\\W_!@#$%^&*`~()-+=]+$)(?![0-9\\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\\W_!@#$%^&*`~()-+=]{8,20}$");
        return pattern.matcher(password).matches();
    }
}
