package com.pet.domains.post.controller;

import com.pet.common.exception.ExceptionMessage;
import com.pet.common.response.ApiResponse;
import com.pet.common.s3.validator.ValidImageSize;
import com.pet.common.util.OptimisticLockingHandlingUtils;
import com.pet.domains.account.domain.Account;
import com.pet.domains.account.domain.LoginAccount;
import com.pet.domains.comment.dto.response.CommentPageResults;
import com.pet.domains.comment.service.CommentService;
import com.pet.domains.post.dto.request.MissingPostCreateParam;
import com.pet.domains.post.dto.request.MissingPostUpdateParam;
import com.pet.domains.post.dto.response.MissingPostReadResult;
import com.pet.domains.post.dto.response.MissingPostReadResults;
import com.pet.domains.post.dto.serach.PostSearchParam;
import com.pet.domains.post.service.MissingPostBookmarkService;
import com.pet.domains.post.service.MissingPostService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/missing-posts")
public class MissingPostController {

    private static final String RETURN_KEY = "id";

    private static final String VIEW_COOKIE_NAME = "view";

    private static final String VIEW_COOKIE_PATH = "/";

    private static final int VIEW_COOKIE_MAX_AGE = 60 * 30;

    private final MissingPostService missingPostService;

    private final MissingPostBookmarkService missingPostBookmarkService;

    private final CommentService commentService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Long>> createMissingPost(
        @RequestPart(value = "images", required = false) @ValidImageSize List<MultipartFile> images,
        @RequestPart(value = "param") @Valid MissingPostCreateParam missingPostCreateParam,
        @LoginAccount Account account
    ) {
        return ApiResponse.ok(
            Map.of(RETURN_KEY, missingPostService.createMissingPost(missingPostCreateParam, images, account)));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<MissingPostReadResults> getMissingPosts(
        @LoginAccount Account account,
        Pageable pageable,
        @Valid PostSearchParam searchParam
    ) {
        if (Objects.nonNull(account)) {
            return ApiResponse.ok(missingPostService.getMissingPostsPageWithAccount(account, pageable, searchParam));
        }
        return ApiResponse.ok(missingPostService.getMissingPostsPage(pageable, searchParam));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(path = "/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<MissingPostReadResult> getMissingPost(
        @LoginAccount Account account,
        @PathVariable Long postId,
        @CookieValue(value = VIEW_COOKIE_NAME, required = false) Cookie viewCookie,
        HttpServletResponse response
    ) {
        boolean shouldIncreaseViewCount = false;
        String cookieValue = formatViewCookieValue(postId);

        if (shouldCreateNewCookie(viewCookie)) {
            shouldIncreaseViewCount = true;
            Cookie newCookie = getNewViewCookie(postId);
            response.addCookie(newCookie);
            log.debug("새 쿠키 생성");
        } else if (notContainsPostId(viewCookie, cookieValue)) {
            shouldIncreaseViewCount = true;
            addViewCookieValueInOriginCookie(viewCookie, response, cookieValue);
            log.debug("기존 뷰 쿠키에 값 추가: {}", viewCookie.getValue());
        } else {
            log.debug("해당 게시글 쿠키 존재");
        }

        MissingPostReadResult result = getMissingPostOneResult(account, postId, shouldIncreaseViewCount)
            .orElseThrow(ExceptionMessage.SERVICE_UNAVAILABLE::getException);
        return ApiResponse.ok(result);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(path = "/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Long>> updateMissingPost(
        @PathVariable Long postId,
        @RequestPart(value = "images", required = false) @ValidImageSize List<MultipartFile> images,
        @RequestPart(value = "param") @Valid MissingPostUpdateParam missingPostUpdateParam,
        @LoginAccount Account account
    ) {
        return ApiResponse.ok(
            Map.of(RETURN_KEY, missingPostService.updateMissingPost(account, postId, missingPostUpdateParam, images)));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{postId}")
    public void deleteMissingPost(@PathVariable Long postId, @LoginAccount Account account) {
        missingPostService.deleteMissingPost(postId, account);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/{postId}/bookmark")
    public void createMissingPostBookmark(@PathVariable Long postId, @LoginAccount Account account) {
        missingPostBookmarkService.createMissingPostBookmark(postId, account);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{postId}/bookmark")
    public void deleteMissingPostBookmark(@PathVariable Long postId, @LoginAccount Account account) {
        missingPostBookmarkService.deleteMissingPostBookmark(postId, account);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(path = "/{postId}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CommentPageResults> getMissingPostComments(@PathVariable Long postId, Pageable pageable) {
        return ApiResponse.ok(commentService.getMissingPostComments(postId, pageable));
    }

    private Optional<MissingPostReadResult> getMissingPostOneResult(
        Account account,
        Long postId,
        boolean shouldIncreaseViewCount
    ) {
        if (Objects.nonNull(account)) {
            return OptimisticLockingHandlingUtils.handling(
                () -> missingPostService.getMissingPostOneWithAccount(account, postId, shouldIncreaseViewCount),
                10,
                "실종/보호 게시물 단건 조회 with jwt"
            );
        }
        return OptimisticLockingHandlingUtils.handling(
            () -> missingPostService.getMissingPostOne(postId, shouldIncreaseViewCount),
            10,
            "실종/보호 게시물 단건 조회"
        );
    }

    private void addViewCookieValueInOriginCookie(Cookie viewCookie, HttpServletResponse response, String cookieValue) {
        viewCookie.setValue(String.format("%s%s", viewCookie.getValue(), cookieValue));
        viewCookie.setMaxAge(VIEW_COOKIE_MAX_AGE);
        viewCookie.setPath(VIEW_COOKIE_PATH);
        response.addCookie(viewCookie);
    }

    private boolean shouldCreateNewCookie(Cookie viewCookie) {
        return Objects.isNull(viewCookie);
    }

    private boolean notContainsPostId(Cookie viewCookie, String viewCookieFormat) {
        return !StringUtils.contains(viewCookie.getValue(), viewCookieFormat);
    }

    private Cookie getNewViewCookie(Long postId) {
        Cookie newCookie = new Cookie(VIEW_COOKIE_NAME, formatViewCookieValue(postId));
        newCookie.setPath("/");
        newCookie.setMaxAge(VIEW_COOKIE_MAX_AGE);
        newCookie.setSecure(true);
        newCookie.setHttpOnly(true);
        return newCookie;
    }

    private String formatViewCookieValue(Long postId) {
        return String.format("[%d]", postId);
    }

}
