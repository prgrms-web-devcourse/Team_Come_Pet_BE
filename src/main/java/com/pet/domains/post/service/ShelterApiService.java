package com.pet.domains.post.service;

import com.pet.common.property.ShelterProperties;
import com.pet.domains.animal.dto.request.AnimalKindCreateParams;
import com.pet.domains.animal.dto.response.AnimalKindApiPageResults;
import com.pet.domains.animal.service.AnimalKindService;
import com.pet.domains.area.dto.request.CityCreateParams;
import com.pet.domains.area.dto.request.CityCreateParams.City;
import com.pet.domains.area.dto.request.TownCreateParams;
import com.pet.domains.area.dto.response.CityApiPageResults;
import com.pet.domains.area.dto.response.TownApiPageResults;
import com.pet.domains.area.service.CityService;
import com.pet.domains.area.service.TownService;
import com.pet.domains.post.dto.response.ShelterApiPageResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class ShelterApiService {

    private static final String SHELTER_POST_PATH = "/abandonmentPublic";

    private static final String ANIMAL_KIND_PATH = "/kind";

    private static final String CITY_PATH = "/sido";

    private static final String TOWN_PATH = "/sigungu";

    private static final List<String> animalKindCodes = List.of("417000", "422400", "429900");

    private static final long NUM_OF_ROWS = 100;

    private final ShelterProperties shelterProperties;

    private final WebClient.Builder webClientBuilder;

    private final ShelterService shelterService;

    private final AnimalKindService animalKindService;

    private final CityService cityService;

    private final TownService townService;

    private WebClient webClient;


    @PostConstruct
    public void initWebClient() {
        String baseUrl = shelterProperties.getUrl();
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        webClient = webClientBuilder
            .uriBuilderFactory(factory)
            .baseUrl(baseUrl)
            .build();
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void shelterPostDailyCronJob() {
        log.info("shelterPostDailyCronJob() start at {}, ", LocalDateTime.now());
        saveShelterApiFirstPageResults();
    }

    public void saveShelterApiFirstPageResults() {
        log.info("saveShelterApiFirstPageResults() start");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String yesterday = LocalDate.now().minusDays(1).format(formatter);

        ShelterApiPageResult result = getShelterApiPageResults(yesterday, yesterday, 1)
            .block();
        shelterService.bulkCreateShelterPosts(Objects.requireNonNull(result, "보호소 게시글 api 응답이 널입니다.").getBodyItems());
        long lastPageNumber = getLastPageNumber(result.getBody().getTotalCount());
        List<Long> pageNumbersForRequest = LongStream.rangeClosed(2, lastPageNumber)
            .boxed()
            .collect(Collectors.toList());
        saveAllShelterApiRemainingPageResults(yesterday, pageNumbersForRequest);
    }

    public void saveAllShelterApiRemainingPageResults(String date, List<Long> pageNumbersForRequest) {
        log.info("saveAllShelterApiRemainingPageResults() start");
        getShelterApiRemainingPageResults(date, date, pageNumbersForRequest)
            .subscribe(response -> {
                shelterService.bulkCreateShelterPosts(response.getBodyItems());
                log.info("Get shelter post api async");
            });
    }

    public Flux<ShelterApiPageResult> getShelterApiRemainingPageResults(String start, String end,
        List<Long> pageNumbers) {
        return Flux.fromIterable(pageNumbers)
            .flatMap(pageNumber -> getShelterApiPageResults(start, end, pageNumber))
            .doOnComplete(() -> log.info("created ShelterApiPageResult flux "));
    }

    public Mono<ShelterApiPageResult> getShelterApiPageResults(
        String start,
        String end,
        long pageNumber
    ) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(SHELTER_POST_PATH)
                .queryParam("serviceKey", shelterProperties.getKey())
                .queryParam("bgnde", start)
                .queryParam("endde", end)
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("pageNo", pageNumber)
                .build())
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .bodyToMono(ShelterApiPageResult.class);
    }

    public void saveAllAnimalKinds() {
        Map<String, AnimalKindCreateParams> createParams = getAllAnimalKindCreateParams();
        createParams.forEach(animalKindService::createAnimalKinds);
    }

    public Map<String, AnimalKindCreateParams> getAllAnimalKindCreateParams() {
        return animalKindCodes.stream()
            .collect(Collectors.toMap(
                kindCode -> kindCode,
                kindCode -> getAnimalKindApiResults(kindCode).getBodyItems())
            );
    }

    public AnimalKindApiPageResults getAnimalKindApiResults(String kind) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(ANIMAL_KIND_PATH)
                .queryParam("serviceKey", shelterProperties.getKey())
                .queryParam("up_kind_cd", kind)
                .build())
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .bodyToMono(AnimalKindApiPageResults.class)
            .block();
    }

    public void saveAllCities() {
        log.info("saveAllCities() cron task start");
        CityCreateParams createParams = getCityCreateParams();
        cityService.createCites(createParams);
    }

    public CityCreateParams getCityCreateParams() {
        return getCityApiPageResults().getBodyItems();
    }

    public CityApiPageResults getCityApiPageResults() {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(CITY_PATH)
                .queryParam("serviceKey", shelterProperties.getKey())
                .queryParam("numOfRows", NUM_OF_ROWS)
                .build())
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .bodyToMono(CityApiPageResults.class)
            .block();
    }

    public void saveAllTowns() {
        log.info("saveAllTowns() cron task start");
        Map<String, TownCreateParams> createParams = getAllTownCreateParams();
        createParams.forEach(townService::createTowns);
    }

    public Map<String, TownCreateParams> getAllTownCreateParams() {
        Map<String, TownCreateParams> paramsMap = new HashMap<>();
        getCityCodes().forEach(cityCode -> {
            TownCreateParams createParams = getTownApiPageResults(cityCode).getBodyItems();
            if (createParams.getTowns() != null) {
                paramsMap.put(cityCode, createParams);
            }
        });
        return paramsMap;
    }

    public Set<String> getCityCodes() {
        CityCreateParams bodyItems = getCityApiPageResults().getBodyItems();
        return bodyItems.getCities().stream()
            .map(City::getCode)
            .collect(Collectors.toSet());
    }

    public TownApiPageResults getTownApiPageResults(String cityCode) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(TOWN_PATH)
                .queryParam("serviceKey", shelterProperties.getKey())
                .queryParam("upr_cd", cityCode)
                .build())
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .bodyToMono(TownApiPageResults.class)
            .block();
    }

    private long getLastPageNumber(long totalCount) {
        long lastPageNumber = (totalCount / NUM_OF_ROWS);
        if (hasRemainder(totalCount)) {
            lastPageNumber++;
        }
        return lastPageNumber;
    }

    private boolean hasRemainder(long divided) {
        return (divided % NUM_OF_ROWS) != 0;
    }
}
