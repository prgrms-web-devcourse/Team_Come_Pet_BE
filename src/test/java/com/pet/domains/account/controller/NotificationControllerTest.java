package com.pet.domains.account.controller;

import static com.pet.domains.docs.utils.ApiDocumentUtils.getDocumentRequest;
import static com.pet.domains.docs.utils.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.pet.domains.account.WithAccount;
import com.pet.domains.account.domain.Account;
import com.pet.domains.account.dto.request.NotificationUpdateParam;
import com.pet.domains.account.dto.response.NotificationReadResults;
import com.pet.domains.docs.BaseDocumentationTest;
import com.pet.domains.post.domain.Status;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("알림 컨트롤러 테스트")
class NotificationControllerTest extends BaseDocumentationTest {

    @Test
    @WithAccount
    @DisplayName("알림 조회 요청 테스트")
    void getNotificationTest() throws Exception {
        // given
        NotificationReadResults result = NotificationReadResults.of(
            List.of(
                NotificationReadResults.Notification.of(
                    1L,
                    "고양이가 멍멍",
                    "http://../../97fd3403-7343-497a-82fa-c41d26ccf0f8.png",
                    513L,
                    Status.DETECTION.name(),
                    "리트리버",
                    "강남구",
                    true
                ),
                NotificationReadResults.Notification.of(
                    2L,
                    "야옹이가 멍멍",
                    "http://../../97fd3403-7343-497a-82fa-c41d26ccf0f8.png",
                    234L,
                    Status.DETECTION.name(),
                    "리트리버",
                    "강남구",
                    true
                ),
                NotificationReadResults.Notification.of(
                    3L,
                    "나홀로 집사",
                    "http://../../97fd3403-7343-497a-82fa-c41d26ccf0f8.png",
                    1231L,
                    Status.DETECTION.name(),
                    "리트리버",
                    "강남구",
                    false
                )
            ), 11, false, 2);

        given(notificationService.getByAccountId(any(Account.class), any(PageRequest.class))).willReturn(result);
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/notices")
            .param("page", "0")
            .param("size", "8")
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("get-notifications",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token"),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                requestParameters(
                    parameterWithName("page").description("페이지 번호"),
                    parameterWithName("size").description("페이지 크기")
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.notifications").type(ARRAY).description("알림"),
                    fieldWithPath("data.notifications[0].id").type(NUMBER).description("알림 id"),
                    fieldWithPath("data.notifications[0].nickname").type(STRING).description("유저 닉네임"),
                    fieldWithPath("data.notifications[0].image").type(STRING).description("유저 프로필 사진"),
                    fieldWithPath("data.notifications[0].postId").type(NUMBER).description("실종/보호 게시물 id"),
                    fieldWithPath("data.notifications[0].status").type(STRING).description("<<status,게시물 상태>>"),
                    fieldWithPath("data.notifications[0].animalKindName").type(STRING).description("품종 이름"),
                    fieldWithPath("data.notifications[0].town").type(STRING).description("시군구 이름"),
                    fieldWithPath("data.notifications[0].checked").type(BOOLEAN).description("알림 읽음 여부"),
                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 게시물 수"),
                    fieldWithPath("data.last").type(BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("data.size").type(NUMBER).description("페이지당 요청 수"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                ))
            );
    }

    @Test
    @WithAccount
    @DisplayName("알림 전체 삭제 요청 테스트")
    void deleteAllNotificationTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/notices")
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("delete-notifications",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                )
            ));
    }


    @Test
    @WithAccount
    @DisplayName("알림 삭제 요청 테스트")
    void deleteNotificationTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/notices/{noticeId}", 1L)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("delete-notification",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                )
            ));
    }

    @Test
    @WithAccount
    @DisplayName("알림 상태 변경 요청 테스트")
    void checkedNotificationTest() throws Exception {
        // given
        NotificationUpdateParam param = new NotificationUpdateParam(true);
        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v1/notices/{noticeId}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(param))
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document("update-notification",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token"),
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                requestFields(
                    fieldWithPath("checked").type(BOOLEAN).description("읽음 여부")
                ))
            );
    }

}
