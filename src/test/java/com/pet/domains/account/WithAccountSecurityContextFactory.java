package com.pet.domains.account;

import com.pet.common.jwt.JwtAuthenticationProvider;
import com.pet.common.jwt.JwtAuthenticationToken;
import com.pet.domains.account.domain.Account;
import com.pet.domains.account.domain.SignStatus;
import com.pet.domains.account.repository.AccountRepository;
import com.pet.domains.account.service.AccountService;
import com.pet.domains.auth.domain.Group;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.authentication.AuthenticationFilter;
import static org.mockito.BDDMockito.*;

@RequiredArgsConstructor
public class WithAccountSecurityContextFactory implements WithSecurityContextFactory<WithAccount> {

    private static final String ROLE_USER = "ROLE_USER";

    private final AccountService accountService;
    private final JwtAuthenticationProvider provider;

    @Override
    public SecurityContext createSecurityContext(WithAccount withAccount) {
        String email = "tester@email.com";
        String password = "user123";
        String nickname = withAccount.value();
        Group group = mock(Group.class);
        Account account = givenAccount(email, nickname, group);

        given(accountService.login(email, password)).willReturn(account);
        given(accountService.checkLoginAccountById(anyLong())).willReturn(account);
        given(group.getAuthorities()).willReturn(List.of((GrantedAuthority)() -> ROLE_USER));

        accountService.signUp(email, password);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(provider.authenticate(new JwtAuthenticationToken(email, password)));
        return context;
    }

    private Account givenAccount(String email, String nickname, Group group) {
        return new Account(
            9127364171L,
            email,
            "$2a$10$21Pd/Fr9GAN9Js6FmvahmuBMEZo73FSBUpDPXl2lTIyLWSqnQoaqi",
            nickname,
            true,
            true,
            SignStatus.SUCCESS,
            null,
            group);
    }

}
