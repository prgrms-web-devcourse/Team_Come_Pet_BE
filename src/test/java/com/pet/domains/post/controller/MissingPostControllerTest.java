package com.pet.domains.post.controller;

import static com.pet.domains.docs.utils.ApiDocumentUtils.getDocumentRequest;
import static com.pet.domains.docs.utils.ApiDocumentUtils.getDocumentResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.pet.domains.account.WithAccount;
import com.pet.domains.account.domain.Account;
import com.pet.domains.comment.dto.response.CommentPageResults;
import com.pet.domains.comment.dto.response.CommentPageResults.Comment;
import com.pet.domains.comment.dto.response.CommentPageResults.Comment.ChildComment;
import com.pet.domains.docs.BaseDocumentationTest;
import com.pet.domains.image.domain.Image;
import com.pet.domains.post.domain.SexType;
import com.pet.domains.post.domain.Status;
import com.pet.domains.post.dto.request.MissingPostCreateParam;
import com.pet.domains.post.dto.request.MissingPostUpdateParam;
import com.pet.domains.post.dto.request.MissingPostUpdateParam.Tag;
import com.pet.domains.post.dto.response.MissingPostReadResult;
import com.pet.domains.post.dto.response.MissingPostReadResults;
import com.pet.domains.post.dto.response.MissingPostReadResults.MissingPost;
import com.pet.domains.post.dto.serach.PostSearchParam;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("실종/보호 게시물 컨트롤러 테스트")
class MissingPostControllerTest extends BaseDocumentationTest {

    @DisplayName("실종/보호 게시물 등록 테스트")
    @WithAccount
    @Test
    void createMissingPostTest() throws Exception {
        //given
        MissingPostCreateParam param = MissingPostCreateParam.of(
            "DETECTION", LocalDate.now(), 1L, 1L, "주민센터 앞 골목 근처",
            "01012343323", 1L, "푸들", 10L, "MALE", "410123456789112",
            "찾아주시면 사례하겠습니다.", List.of(
                MissingPostCreateParam.Tag.of("춘식이")
            )
        );

        //when
        MockMultipartFile firstMultipartFile =
            new MockMultipartFile("images", "", "multipart/form-data", "abcd.jpg".getBytes());
        MockMultipartFile secondMultipartFile =
            new MockMultipartFile("images", "", "multipart/form-data", "abcd2.jpg".getBytes());
        MockMultipartFile paramFile =
            new MockMultipartFile("param", "", "application/json", objectMapper.writeValueAsString(param).getBytes(
                StandardCharsets.UTF_8));

        given(imageService.createImage(firstMultipartFile)).willReturn(mock(Image.class));

        ResultActions resultActions = mockMvc.perform(multipart("/api/v1/missing-posts")
            .file(firstMultipartFile)
            .file(secondMultipartFile)
            .file(paramFile)
            .contentType(MediaType.MULTIPART_MIXED)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken())
            .characterEncoding("UTF-8")
        );

        //then
        resultActions
            .andDo(print())
            .andExpect(status().isCreated())
            .andDo(document("create-missing-post",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token"),
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                requestParts(
                    partWithName("images").description("게시글 이미지"),
                    partWithName("param").description("게시글 등록 요청 데이터")
                ),
                requestPartFields(
                    "param",
                    fieldWithPath("status").type(STRING).description("<<status,게시물 상태>>"),
                    fieldWithPath("date").type(STRING).description("발견 날짜"),
                    fieldWithPath("cityId").type(NUMBER).description("시도 id"),
                    fieldWithPath("townId").type(NUMBER).description("시군구 id"),
                    fieldWithPath("detailAddress").type(STRING).description("상세 및 추가 주소").optional(),
                    fieldWithPath("telNumber").type(STRING).description("연락처"),
                    fieldWithPath("animalId").type(NUMBER).description("동물 id"),
                    fieldWithPath("animalKindName").type(STRING).description("품종 이름"),
                    fieldWithPath("age").type(NUMBER).description("나이"),
                    fieldWithPath("sex").type(STRING).description("<<sexType,동물 성별>>"),
                    fieldWithPath("chipNumber").type(STRING).description("칩번호").optional(),
                    fieldWithPath("content").type(STRING).description("게시물 내용"),
                    fieldWithPath("tags").type(ARRAY).description("게시글의 해시태그들").optional(),
                    fieldWithPath("tags[0].name").type(STRING).description("해시태그 내용").optional()
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터").optional(),
                    fieldWithPath("data.id").type(NUMBER).description("게시글 id"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")
                )
            ));
    }

    @Test
    @WithAccount
    @DisplayName("실종/보호 게시물 리스트 조회 테스트")
    void getMissingPostsTest() throws Exception {
        //given
        MissingPostReadResults missingPostReadResults = MissingPostReadResults.of(List.of(
            MissingPost.of(
                1L, "서울특별시", "도봉구", "토이푸들", Status.DETECTION, LocalDateTime.now(),
                SexType.FEMALE, true, 2,
                "https://post-phinf.pstatic.net/MjAyMTA0MTJfNTAg/MDAxNjE4MjMwNjg1MTEw",
                List.of(
                    MissingPost.Tag.of(1L, "고슴도치"),
                    MissingPost.Tag.of(2L, "애완동물"),
                    MissingPost.Tag.of(3L, "반려동물")
                )
            )),
            10,
            true,
            5
        );
        given(missingPostService.getMissingPostsPageWithAccount(any(Account.class), any(PageRequest.class), any(
            PostSearchParam.class))).willReturn(
            missingPostReadResults);

        //when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/missing-posts")
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken())
            .param("page", "1")
            .param("size", "10")
            .param("sort", "id,DESC"));

        // then
        resultActions
            .andExpect(status().isOk())
            .andDo(document("get-missing-posts",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token - optional").optional()
                ),
                requestParameters(
                    parameterWithName("page").description("페이지 번호"),
                    parameterWithName("size").description("페이지 크기"),
                    parameterWithName("sort").description("정렬, ex) id,[desc]")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.missingPosts").type(ARRAY).description("실종/보호 게시물 리스트"),
                    fieldWithPath("data.missingPosts[].id").type(NUMBER).description("게시글 id"),
                    fieldWithPath("data.missingPosts[].city").type(STRING).description("시도 이름"),
                    fieldWithPath("data.missingPosts[].town").type(STRING).description("시군구 이름"),
                    fieldWithPath("data.missingPosts[].animalKindName").type(STRING).description("동물 품종 이름"),
                    fieldWithPath("data.missingPosts[].status").type(STRING).description("<<status,게시물 상태>>"),
                    fieldWithPath("data.missingPosts[].createdAt").type(STRING).description("게시글 작성날짜"),
                    fieldWithPath("data.missingPosts[].sex").type(STRING).description("<<sexType,동물 성별>>"),
                    fieldWithPath("data.missingPosts[].thumbnail").type(STRING).description("게시글 썸네일"),
                    fieldWithPath("data.missingPosts[].isBookmark").type(BOOLEAN).description("북마크 여부"),
                    fieldWithPath("data.missingPosts[].bookmarkCount").type(NUMBER).description("북마크 수"),
                    fieldWithPath("data.missingPosts[].tags").type(ARRAY).description("해시태그 배열"),
                    fieldWithPath("data.missingPosts[].tags[].id").type(NUMBER).description("해시태그 id"),
                    fieldWithPath("data.missingPosts[].tags[].name").type(STRING).description("해시태그 내용"),
                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 게시물 수"),
                    fieldWithPath("data.last").type(BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("data.size").type(NUMBER).description("페이지당 요청 수"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")))
            );
    }

    @Test
    @WithAccount
    @DisplayName("실종/보호 게시물 단건 조회 테스트")
    void getMissingPostTest() throws Exception {
        //given
        MissingPostReadResult missingPostReadResult = MissingPostReadResult.of(1L,
            MissingPostReadResult.Account.of(1L, "짱구",
                "https://img.insight.co.kr/static/2021/01/10/700/img_20210110130830_kue82l80.webp"
            ),
            Status.DETECTION, "2021-11-11", "경기도", "구리시", "주민센터 앞 골목 근처",
            "01032430012", "개", "리트리버", 10, SexType.MALE,
            "410123456789112",
            List.of(
                MissingPostReadResult.Image.of(1L, "http://../../97fd3403-7343-497a-82fa-c41d26ccf0f8.png"),
                MissingPostReadResult.Image.of(2L, "http://../../97fd3403-7343-497a-82fa-c41d26ccf0f8.png")
            ),
            List.of(
                MissingPostReadResult.Tag.of(1L, "해시태그"),
                MissingPostReadResult.Tag.of(2L, "춘식이")
            ),
            "찾아주시면 반드시 사례하겠습니다. 연락주세요", 3, 1, true, 1, LocalDateTime.now()
        );
        given(missingPostService.getMissingPostOneWithAccount(any(Account.class), anyLong(), anyBoolean())).willReturn(
            missingPostReadResult);

        //when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/missing-posts/{postId}", 1L)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andExpect(status().isOk())
            .andDo(document("get-missing-post",
                getDocumentRequest(),
                getDocumentResponse(),
                pathParameters(
                    parameterWithName("postId").description("게시글 id")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.id").type(NUMBER).description("게시글 id"),
                    fieldWithPath("data.account").type(OBJECT).description("게시글 작성자"),
                    fieldWithPath("data.account.id").type(NUMBER).description("게시글 작성자 id"),
                    fieldWithPath("data.account.nickname").type(STRING).description("게시글 작성자 닉네임"),
                    fieldWithPath("data.account.image").type(STRING).description("게시글 작성자 프로필 url"),
                    fieldWithPath("data.status").type(STRING).description("<<status,게시물 상태>>"),
                    fieldWithPath("data.date").type(STRING).description("상태 날짜"),
                    fieldWithPath("data.city").type(STRING).description("시도 이름"),
                    fieldWithPath("data.town").type(STRING).description("시군구 이름"),
                    fieldWithPath("data.detailAddress").type(STRING).description("상세 및 추가 주소"),
                    fieldWithPath("data.telNumber").type(STRING).description("연락처"),
                    fieldWithPath("data.animal").type(STRING).description("동물 종류"),
                    fieldWithPath("data.animalKindName").type(STRING).description("동물 품종 이름"),
                    fieldWithPath("data.age").type(NUMBER).description("동물 나이"),
                    fieldWithPath("data.sex").type(STRING).description("<<sexType,동물 성별>>"),
                    fieldWithPath("data.chipNumber").type(STRING).description("칩번호"),
                    fieldWithPath("data.images").type(ARRAY).description("이미지들"),
                    fieldWithPath("data.images[].id").type(NUMBER).description("이미지 id"),
                    fieldWithPath("data.images[].name").type(STRING).description("이미지 url"),
                    fieldWithPath("data.tags").type(ARRAY).description("해시태그 배열"),
                    fieldWithPath("data.tags[].id").type(NUMBER).description("해시태그 id"),
                    fieldWithPath("data.tags[].name").type(STRING).description("해시태그 값"),
                    fieldWithPath("data.content").type(STRING).description("게시글 내용"),
                    fieldWithPath("data.viewCount").type(NUMBER).description("조회수"),
                    fieldWithPath("data.bookmarkCount").type(NUMBER).description("북마크 수"),
                    fieldWithPath("data.isBookmark").type(BOOLEAN).description("북마크 여부"),
                    fieldWithPath("data.commentCount").type(NUMBER).description("댓글 수"),
                    fieldWithPath("data.createdAt").type(STRING).description("게시글 작성날짜"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")))
            );
    }

    @Test
    @WithAccount
    @DisplayName("실종/보호 게시물 수정 테스트")
    void updateMissingPostTest() throws Exception {
        //given
        MissingPostUpdateParam param = MissingPostUpdateParam.of(
            Status.DETECTION, LocalDate.now(), 1L, 1L, "주민센터 앞 골목 근처", "01034231111",
            1L, "푸들", 10L, SexType.MALE, "410123456789112",
            List.of(
                Tag.of("춘식이")
            ),
            "찾아주시면 반드시 사례하겠습니다. 연락주세요.",
            List.of(
                MissingPostUpdateParam.Image.of(1L, "abcddeee.jpg")
            )
        );
        MockMultipartFile multipartFile =
            new MockMultipartFile("images", "", "multipart/form-data", "abcd2.jpg".getBytes());
        MockMultipartFile paramFile =
            new MockMultipartFile("param", "", "application/json", objectMapper.writeValueAsString(param).getBytes(
                StandardCharsets.UTF_8));

        //when
        ResultActions resultActions = mockMvc.perform(multipart("/api/v1/missing-posts/{postId}", 1L)
            .file(multipartFile)
            .file(paramFile)
            .contentType(MediaType.MULTIPART_MIXED)
            .accept(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken())
            .characterEncoding("UTF-8"));

        //then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("update-missing-post",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token"),
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE),
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                requestParts(
                    partWithName("images").description("게시글 이미지"),
                    partWithName("param").description("게시글 등록 요청 데이터")
                ),
                requestPartFields(
                    "param",
                    fieldWithPath("status").type(STRING).description("<<status,게시물 상태>>"),
                    fieldWithPath("date").type(STRING).description("발견 날짜"),
                    fieldWithPath("cityId").type(NUMBER).description("시도 id"),
                    fieldWithPath("townId").type(NUMBER).description("시군구 id"),
                    fieldWithPath("detailAddress").type(STRING).description("상세 및 추가 주소").optional(),
                    fieldWithPath("telNumber").type(STRING).description("연락처"),
                    fieldWithPath("animalId").type(NUMBER).description("동물 id"),
                    fieldWithPath("animalKindName").type(STRING).description("품종 이름"),
                    fieldWithPath("age").type(NUMBER).description("나이"),
                    fieldWithPath("sex").type(STRING).description("<<sexType,동물 성별>>"),
                    fieldWithPath("chipNumber").type(STRING).description("칩번호").optional(),
                    fieldWithPath("content").type(STRING).description("게시물 내용"),
                    fieldWithPath("tags").type(ARRAY).description("게시글의 해시태그들").optional(),
                    fieldWithPath("tags[0].name").type(STRING).description("해시태그 내용").optional(),
                    fieldWithPath("images").type(ARRAY).description("게시글의 이미지들").optional(),
                    fieldWithPath("images[0].id").type(NUMBER).description("이미지 id").optional(),
                    fieldWithPath("images[0].name").type(STRING).description("이미지 url").optional()
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.id").type(NUMBER).description("실종/보호 게시물 id"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")))
            );

    }

    @Test
    @WithAccount
    @DisplayName("실종/보호 게시물 삭제 테스트")
    void deleteMissingPostTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/missing-posts/{postId}", 1L)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andExpect(status().isNoContent())
            .andDo(document("delete-missing-post",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                pathParameters(
                    parameterWithName("postId").description("실종/보호 게시물 id")
                ))
            );
    }

    @Test
    @WithAccount
    @DisplayName("실종/보호 게시글 북마크 생성 테스트")
    void createMissingPostBookmarkTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v1/missing-posts/{postId}/bookmark", 1L)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andExpect(status().isCreated())
            .andDo(document("create-missing-post-bookmark",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                pathParameters(
                    parameterWithName("postId").description("실종/보호 게시글 id")
                ))
            );
    }

    @Test
    @WithAccount
    @DisplayName("실종/보호 게시글 북마크 삭제 테스트")
    void deleteMissingPostBookmarkTest() throws Exception {
        // given
        // when
        ResultActions resultActions = mockMvc.perform(delete("/api/v1/missing-posts/{postId}/bookmark", 1L)
            .header(HttpHeaders.AUTHORIZATION, getAuthenticationToken()));

        // then
        resultActions
            .andExpect(status().isNoContent())
            .andDo(document("delete-missing-post-bookmark",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.AUTHORIZATION).description("jwt token")
                ),
                pathParameters(
                    parameterWithName("postId").description("실종/보호 게시글 id")
                ))
            );
    }

    @Test
    @DisplayName("실종 게시글의 댓글 페이지 조회 테스트")
    void getMissingPostCommentsTest() throws Exception {
        // given
        CommentPageResults commentPageResults = new CommentPageResults(
            LongStream.rangeClosed(1, 2).mapToObj(idx -> new CommentPageResults.Comment(
                idx,
                "부모 댓글 #" + idx,
                LocalDateTime.now(),
                new Comment.Account(idx, "회원#" + idx, "http://../.jpg"),
                List.of(new ChildComment(
                    idx * 3,
                    "자식 댓글 #" + idx * 3,
                    LocalDateTime.now(),
                    new Comment.Account(idx * 3, "회원#" + idx * 3, "http://../.jpg"))
                ),
                false))
                .collect(Collectors.toList()),
            2,
            true,
            10
        );
        given(commentService.getMissingPostComments(anyLong(), any(PageRequest.class))).willReturn(commentPageResults);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v1/missing-posts/{postId}/comments", 1L)
            .accept(MediaType.APPLICATION_JSON_VALUE));

        // then
        resultActions
            .andDo(print())
            .andExpect(status().isOk())
            .andDo(document("get-missing-post-comments",
                getDocumentRequest(),
                getDocumentResponse(),
                requestHeaders(
                    headerWithName(HttpHeaders.ACCEPT).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                pathParameters(
                    parameterWithName("postId").description("실종 게시글 아이디")
                ),
                responseHeaders(
                    headerWithName(HttpHeaders.CONTENT_TYPE).description(MediaType.APPLICATION_JSON_VALUE)
                ),
                responseFields(
                    fieldWithPath("data").type(OBJECT).description("응답 데이터"),
                    fieldWithPath("data.comments").type(ARRAY).description("댓글 목록"),
                    fieldWithPath("data.comments[].id").type(NUMBER).description("댓글 아이디"),
                    fieldWithPath("data.comments[].content").type(STRING).description("댓글 내용"),
                    fieldWithPath("data.comments[].createdAt").type(STRING).description("댓글 작성날짜"),
                    fieldWithPath("data.comments[].deleted").type(BOOLEAN).description("삭제된 댓글 여부"),
                    fieldWithPath("data.comments[].account").type(OBJECT).description("댓글 작성자"),
                    fieldWithPath("data.comments[].account.id").type(NUMBER).description("작성자 아이디"),
                    fieldWithPath("data.comments[].account.nickname").type(STRING).description("작성자 닉네임"),
                    fieldWithPath("data.comments[].account.image").type(STRING).description("작성자 프로필 사진"),
                    fieldWithPath("data.comments[].childComments").type(ARRAY).description("댓글의 대댓글 목록"),
                    fieldWithPath("data.comments[].childComments[].id").type(NUMBER).description("대댓글 아이디"),
                    fieldWithPath("data.comments[].childComments[].content").type(STRING).description("대댓글 내용"),
                    fieldWithPath("data.comments[].childComments[].createdAt").type(STRING).description("대댓글 작성날짜"),
                    fieldWithPath("data.comments[].childComments[].account").type(OBJECT).description("대댓글 작성자"),
                    fieldWithPath("data.comments[].childComments[].account.id").type(NUMBER).description("작성자 아이디"),
                    fieldWithPath("data.comments[].childComments[].account.nickname").type(STRING)
                        .description("작성자 닉네임"),
                    fieldWithPath("data.comments[].childComments[].account.image").type(STRING)
                        .description("작성자 프로필 사진"),
                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 결과 수"),
                    fieldWithPath("data.last").type(BOOLEAN).description("마지막 페이지 여부"),
                    fieldWithPath("data.size").type(NUMBER).description("페이지당 요청 수"),
                    fieldWithPath("serverDateTime").type(STRING).description("서버 응답 시간")))
            );
    }

}
