package com.pet.domains.account.controller;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.common.jwt.JwtMockToken;
import com.pet.domains.account.dto.request.AccountAreaUpdateParam;
import com.pet.domains.account.dto.request.AccountCreateParam;
import com.pet.domains.account.dto.request.AccountEmailParam;
import com.pet.domains.account.dto.request.AccountLonginParam;
import com.pet.domains.account.dto.request.AccountPasswordParam;
import com.pet.domains.account.dto.request.AccountUpdateParam;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(value = AccountController.class)
@AutoConfigureRestDocs
@DisplayName("회원 컨트롤러 테스트")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("이메일 인증 요청 테스트")
    void emailVerifyTest() throws Exception {
        // given
        AccountEmailParam param = new AccountEmailParam("tester@email.com");
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param)));

        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("verify-email",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ))
            );
    }

    @Test
    @DisplayName("회원 가입 요청 성공 테스트")
    void signUpTest() throws Exception {
        // given
        AccountCreateParam param = new AccountCreateParam("tester", "tester@email.com", "12345678a!");
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/sign-up")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param)));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isCreated())
            .andDo(document("sign-up",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터").optional(),
                    fieldWithPath("data.id").type(NUMBER).description("회원 id"),
                    fieldWithPath("data.token").type(STRING).description("토큰"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginTest() throws Exception {
        // given
        AccountLonginParam param = new AccountLonginParam("tester@email.com", "12345678a!");
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param)));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isCreated())
            .andDo(document("login",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터").optional(),
                    fieldWithPath("data.id").type(NUMBER).description("회원 id"),
                    fieldWithPath("data.token").type(STRING).description("토큰"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );
    }

    @Test
    @DisplayName("로그아웃 테스트")
    void logoutTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/logout")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("logout",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ))
            );
    }

    @Test
    @DisplayName("회원 정보 수정 테스트")
    void updateAccountTest() throws Exception {
        // given
        AccountUpdateParam param = new AccountUpdateParam(1L, "otherNickname");
        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v1/me")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param)));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("update-account",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                requestFields(
                    fieldWithPath("id").type(NUMBER).description("회원 id"),
                    fieldWithPath("nickname").type(STRING).description("닉네임")
                ))
            );
    }

    @Test
    @DisplayName("회원 정보 수정 테스트")
    void updatePasswordTest() throws Exception {
        // given
        AccountPasswordParam param = new AccountPasswordParam("otherPassword12!");
        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v1/change-password")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param)));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("update-password",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                requestFields(
                    fieldWithPath("password").type(STRING).description("비밀번호")
                ))
            );
    }

    @Test
    @DisplayName("회원의 관심 지역 조회 테스트")
    void getAccountAreaTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/me/areas")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("get-account-areas",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.areas").type(ARRAY).description("시도"),
                    fieldWithPath("data.areas[0].cityId").type(NUMBER).description("시도 id"),
                    fieldWithPath("data.areas[0].cityName").type(STRING).description("시도 이름"),
                    fieldWithPath("data.areas[0].townId").type(NUMBER).description("시군구 id"),
                    fieldWithPath("data.areas[0].townName").type(STRING).description("시군구 이름"),
                    fieldWithPath("data.areas[0].defaultArea").type(BOOLEAN).description("디폴트 지역 여부"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );

    }

    @Test
    @DisplayName("관심 지역 설정 테스트")
    void updateAccountAreaTest() throws Exception {
        // given
        AccountAreaUpdateParam param = new AccountAreaUpdateParam(
            List.of(
                AccountAreaUpdateParam.Area.of(1L, 1L, true),
                AccountAreaUpdateParam.Area.of(1L, 2L, false)
            ), true
        );
        // when
        ResultActions resultActions = mockMvc.perform(put("/api/v1/me/areas")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param)));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("update-account-areas",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                requestFields(
                    fieldWithPath("areas").type(ARRAY).description("시도"),
                    fieldWithPath("areas[0].cityId").type(NUMBER).description("시도 id"),
                    fieldWithPath("areas[0].townId").type(NUMBER).description("시군구 id"),
                    fieldWithPath("areas[0].defaultArea").type(BOOLEAN).description("디폴트 지역 여부"),
                    fieldWithPath("notification").type(BOOLEAN).description("알림 여부")
                ))
            );
    }

    @Test
    @DisplayName("회원 게시물 조회")
    void getAccountMissingPostsTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/me/posts")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN)
            .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("get-account-missing-posts",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.posts").type(ARRAY).description("게시물"),
                    fieldWithPath("data.posts[0].id").type(NUMBER).description("게시물 id"),
                    fieldWithPath("data.posts[0].city").type(STRING).description("시도 이름"),
                    fieldWithPath("data.posts[0].town").type(STRING).description("시군구 이름"),
                    fieldWithPath("data.posts[0].animalKind").type(STRING).description("품종"),
                    fieldWithPath("data.posts[0].status").type(STRING).description("게시물 상태"),
                    fieldWithPath("data.posts[0].date").type(STRING).description("게시물 등록 날짜"),
                    fieldWithPath("data.posts[0].sex").type(STRING).description("성별"),
                    fieldWithPath("data.posts[0].isBookmark").type(BOOLEAN).description("북마크 여부"),
                    fieldWithPath("data.posts[0].bookmarkCount").type(NUMBER).description("북마크 수"),
                    fieldWithPath("data.posts[0].postTags").type(ARRAY).description("게시물 태그"),
                    fieldWithPath("data.posts[0].postTags[0].id").type(NUMBER).description("게시물 태그 id"),
                    fieldWithPath("data.posts[0].postTags[0].name").type(STRING).description("게시물 태그 이름"),
                    fieldWithPath("data.posts[0].thumbnail").type(STRING).description("게시물 썸네일"),
                    fieldWithPath("data.totalElements").type(NUMBER).description("총 게시물 수"),
                    fieldWithPath("data.last").type(BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );

    }

    @Test
    @DisplayName("회원 북마크(실종/보호) 조회")
    void getAccountMissingBookmarkPostTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/me/bookmarks?status=missing")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN)
            .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("get-account-missing-bookmark-posts",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                requestParameters(
                    parameterWithName("status").description("게시물 종류")
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.posts").type(ARRAY).description("게시물"),
                    fieldWithPath("data.posts[0].id").type(NUMBER).description("게시물 id"),
                    fieldWithPath("data.posts[0].animalKind").type(STRING).description("품종"),
                    fieldWithPath("data.posts[0].sexType").type(STRING).description("성별"),
                    fieldWithPath("data.posts[0].place").type(STRING).description("위치"),
                    fieldWithPath("data.posts[0].createdAt").type(STRING).description("게시물 생성일"),
                    fieldWithPath("data.posts[0].sexType").type(STRING).description("성별"),
                    fieldWithPath("data.posts[0].thumbnail").type(STRING).description("게시물 썸네일"),
                    fieldWithPath("data.posts[0].bookmarkCount").type(NUMBER).description("북마크 수"),
                    fieldWithPath("data.totalElements").type(NUMBER).description("총 게시물 수"),
                    fieldWithPath("data.last").type(BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );

    }

    @Test
    @DisplayName("회원 북마크(보호소) 조회")
    void getAccountShelterBookmarkPostTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/me/bookmarks?status=shelter")
            .header(HttpHeaders.AUTHORIZATION, JwtMockToken.MOCK_TOKEN)
            .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("get-account-shelter-bookmark-posts",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                requestParameters(
                    parameterWithName("status").description("게시물 종류")
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.posts").type(ARRAY).description("게시물"),
                    fieldWithPath("data.posts[0].id").type(NUMBER).description("게시물 id"),
                    fieldWithPath("data.posts[0].animalKind").type(STRING).description("품종"),
                    fieldWithPath("data.posts[0].sexType").type(STRING).description("성별"),
                    fieldWithPath("data.posts[0].place").type(STRING).description("위치"),
                    fieldWithPath("data.posts[0].createdAt").type(STRING).description("게시물 생성일"),
                    fieldWithPath("data.posts[0].sexType").type(STRING).description("성별"),
                    fieldWithPath("data.posts[0].thumbnail").type(STRING).description("게시물 썸네일"),
                    fieldWithPath("data.posts[0].bookmarkCount").type(NUMBER).description("북마크 수"),
                    fieldWithPath("data.totalElements").type(NUMBER).description("총 게시물 수"),
                    fieldWithPath("data.last").type(BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("data.size").type(NUMBER).description("페이지 크기"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );

    }

    @Test
    @DisplayName("회원 탈퇴 테스트")
    void deleteAccount() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/me"));
        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("delete-account",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())
            ));
    }

}
