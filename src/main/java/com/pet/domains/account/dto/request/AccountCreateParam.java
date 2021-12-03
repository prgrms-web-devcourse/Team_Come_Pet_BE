package com.pet.domains.account.dto.request;

import lombok.Getter;

@Getter
public class AccountCreateParam {

    private String nickname;

    private String email;

    private String password;

    public AccountCreateParam(String nickname, String email, String password) {
        this.nickname = nickname;
        this.email = email;
        this.password = password;
    }

}
