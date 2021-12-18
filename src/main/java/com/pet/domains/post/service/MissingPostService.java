package com.pet.domains.post.service;

import com.pet.common.exception.ExceptionMessage;
import com.pet.common.util.OptimisticLockingHandlingUtils;
import com.pet.domains.account.domain.Account;
import com.pet.domains.animal.domain.AnimalKind;
import com.pet.domains.animal.service.AnimalKindService;
import com.pet.domains.area.domain.Town;
import com.pet.domains.area.repository.TownRepository;
import com.pet.domains.comment.repository.CommentRepository;
import com.pet.domains.image.domain.Image;
import com.pet.domains.image.domain.PostImage;
import com.pet.domains.image.repository.PostImageRepository;
import com.pet.domains.image.service.ImageService;
import com.pet.domains.post.domain.MissingPost;
import com.pet.domains.post.dto.request.MissingPostCreateParam;
import com.pet.domains.post.dto.request.MissingPostUpdateParam;
import com.pet.domains.post.dto.response.MissingPostReadResult;
import com.pet.domains.post.dto.response.MissingPostReadResults;
import com.pet.domains.post.mapper.MissingPostMapper;
import com.pet.domains.post.repository.MissingPostRepository;
import com.pet.domains.post.repository.MissingPostWithIsBookmark;
import com.pet.domains.tag.domain.PostTag;
import com.pet.domains.tag.domain.Tag;
import com.pet.domains.tag.repository.PostTagRepository;
import com.pet.domains.tag.service.PostTagService;
import com.pet.domains.tag.service.TagService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MissingPostService {

    private final MissingPostRepository missingPostRepository;

    private final AnimalKindService animalKindService;

    private final TownRepository townRepository;

    private final PostImageRepository postImageRepository;

    private final PostTagService postTagService;

    private final PostTagRepository postTagRepository;

    private final TagService tagService;

    private final MissingPostMapper missingPostMapper;

    private final ImageService imageService;

    private final CommentRepository commentRepository;

    @Transactional
    public Long createMissingPost(MissingPostCreateParam missingPostCreateParam, List<MultipartFile> multipartFiles,
        Account account) {
        if (multipartFiles.size() > 3) {
            throw ExceptionMessage.INVALID_IMAGE_COUNT.getException();
        }
        AnimalKind animalKind = animalKindService.getOrCreateAnimalKind(missingPostCreateParam.getAnimalId(),
            missingPostCreateParam.getAnimalKindName());
        Town town = townRepository.getById(missingPostCreateParam.getTownId());

        List<Tag> tags = getTags(missingPostCreateParam);
        List<Image> imageFiles = uploadAndGetImages(multipartFiles);
        String thumbnail = getThumbnail(imageFiles);

        MissingPost mappingMissingPost =
            missingPostMapper.toEntity(missingPostCreateParam, town, animalKind, thumbnail, account);

        if (!CollectionUtils.isEmpty(tags)) {
            postTagService.createPostTag(tags, mappingMissingPost);
        }
        createPostImage(imageFiles, mappingMissingPost);

        return missingPostRepository.save(mappingMissingPost).getId();
    }

    @Transactional
    public void deleteMissingPost(Long postId, Account account) {
        MissingPost getMissingPost = missingPostRepository.findById(postId)
            .filter(post -> post.getAccount().getId().equals(account.getId()))
            .orElseThrow(ExceptionMessage.UN_IDENTIFICATION::getException);
        commentRepository.deleteAllByMissingPostId(getMissingPost.getId());

        List<PostTag> getPostTags = postTagRepository.getPostTagsByMissingPostId(getMissingPost.getId());
        OptimisticLockingHandlingUtils.handling(
            () -> tagService.decreaseTagCount(getPostTags),
            5,
            "게시글 삭제시 태그 카운트 감소"
        );

        missingPostRepository.deleteById(getMissingPost.getId());
    }

    public MissingPostReadResults getMissingPostsPage(Pageable pageable) {
        Page<MissingPost> pageResult = missingPostRepository.findAllWithFetch(pageable);
        return missingPostMapper.toMissingPostsResults(pageResult);
    }

    public MissingPostReadResults getMissingPostsPageWithAccount(Account account, Pageable pageable) {
        Page<MissingPostWithIsBookmark> pageResult =
            missingPostRepository.findAllWithIsBookmarkAccountByDeletedIsFalse(account, pageable);
        return missingPostMapper.toMissingPostsWithBookmarkResults(pageResult);
    }

    @Transactional
    public MissingPostReadResult getMissingPostOne(Long postId) {
        MissingPost missingPost =
            missingPostRepository.findByMissingPostId(postId)
                .orElseThrow(ExceptionMessage.NOT_FOUND_MISSING_POST::getException);
        missingPost.increaseViewCount();
        return missingPostMapper.toMissingPostDto(missingPost);
    }

    @Transactional
    public MissingPostReadResult getMissingPostOneWithAccount(Account account, Long postId) {
        MissingPostWithIsBookmark missingPostWithIsBookmark =
            missingPostRepository.findByIdAndWithIsBookmarkAccount(account, postId);
        missingPostWithIsBookmark.getMissingPost().increaseViewCount();
        return missingPostMapper.toMissingPostDto(missingPostWithIsBookmark);
    }

    @Transactional
    public Long updateMissingPost(Account account, Long postId, MissingPostUpdateParam param,
        List<MultipartFile> images) {
        //1. 게시글 조회
        MissingPost missingPost =
            missingPostRepository.findById(postId).orElseThrow(ExceptionMessage.NOT_FOUND_MISSING_POST::getException);

        //image
        //기존에 있는 이미지면 s3 url
        //multipart에 담는지, url로 주는지
        //image 2개로
        //기존거는 body, 새거는 multipart

        //없으면 ""
        //새로운 이미지면 파일명


        //2. param으로 가져온 tag들 현재 list와 비교하기
        //O
        List<String> getTagsFromParam =
            param.getTags().stream().map(MissingPostUpdateParam.Tag::getName).collect(Collectors.toList());

        //비교를 이렇게 하면 안될거같은데
        List<String> getTagsFromEntity =
            missingPost.getPostTags().stream().map(postTag -> postTag.getTag().getName()).collect(Collectors.toList());

        //중복 제거
        //기존 1, 2, 2
        //새로 2, 4, 5

        //a - b
        //b - a
        //collection


        //2-1 같으면 교체 없이 그대로 진행

        //2-2 다른 tag가 있다면 기존 태그 posttag와 tag에서 개수 감소하고, 새로운 태그 tag 개수 추가 및 posttag 추가

        //3. param으로 가져온 image들 현재 list와 비교하기

        //3-1 같으면 교체 없이 그대로 진행

        //3-2 다른 image가 있다면 기존 postImage에서 제거하고, 새로운 image 추가 및 postImage 추가

        //4. param으로 가져온 값들 넣가주기
        missingPost.changeInfo(param.getStatus(), param.getDate(), param.getCity(), param.getTown(),
            param.getDetailAddress(), param.getTelNumber(), param.getAnimal(), param.getAnimalKindName(),
            param.getAge(), param.getSex(), param.getChipNumber(), param.getContent());

        return missingPost.getId();
    }

    private String getThumbnail(List<Image> imageFiles) {
        String thumbnail = null;
        if (!CollectionUtils.isEmpty(imageFiles)) {
            thumbnail = imageFiles.get(0).getName();
        }
        return thumbnail;
    }

    private void createPostImage(List<Image> imageFiles, MissingPost mappingMissingPost) {
        if (!CollectionUtils.isEmpty(imageFiles)) {
            imageFiles.forEach(image -> PostImage.builder()
                .missingPost(mappingMissingPost)
                .image(image)
                .build());
        }
    }

    private List<Image> uploadAndGetImages(List<MultipartFile> multipartFiles) {
        return multipartFiles.stream()
            .filter(multipartFile -> !StringUtils.isEmpty(multipartFile.getOriginalFilename()))
            .map(imageService::createImage).collect(Collectors.toList());
    }

    private List<Tag> getTags(MissingPostCreateParam missingPostCreateParam) {
        return Objects.requireNonNull(missingPostCreateParam.getTags())
            .stream()
            .map(tag -> tagService.getOrCreateByTagName(tag.getName()))
            .collect(Collectors.toList());
    }

}
