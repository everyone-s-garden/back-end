package com.everyonegarden.garden;

import com.everyonegarden.common.exception.BadRequestException;
import com.everyonegarden.common.memberId.MemberId;
import com.everyonegarden.garden.dto.*;
import com.everyonegarden.garden.model.Garden;
import com.everyonegarden.garden.s3.S3Service;
import com.everyonegarden.gardenView.GardenViewService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("v1/garden")
public class GardenControllerV1 {

    private final GardenService gardenService;
    private final GardenViewService gardenViewService;

    private final S3Service s3Service;

    @GetMapping("{type}/by-region")
    public List<GardenResponse> getPublicGardenByRegion(@PathVariable("type") String type,
                                                        @RequestParam("region") String region) {
        GardenTypeRequest gardenTypeRequest;
        try {
            gardenTypeRequest = GardenTypeRequest.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException(String.format("%s은 올바른 범위가 아닙니다", type));
        }

        if (gardenTypeRequest == GardenTypeRequest.PUBLIC) {
            return gardenService.getPublicGardenByRegion(region);
        }

        if (gardenTypeRequest == GardenTypeRequest.RPIVATE) {
            return gardenService.getPrivateGardenByRegion(region);
        }

        return gardenService.getAllGardenByRegion(region);
    }

    @GetMapping("{type}/by-coordinate")
    public List<GardenResponse> getPublicGardenByCoordinate(@PathVariable("type") String type,
                                                            @RequestParam("lat") String latitude,
                                                            @RequestParam("long") String longitude) {
        GardenTypeRequest gardenTypeRequest;
        try {
            gardenTypeRequest = GardenTypeRequest.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException(String.format("%s은 올바른 범위가 아닙니다", type));
        }

        double latStart = Double.parseDouble(latitude.split(",")[0]);
        double latEnd = Double.parseDouble(latitude.split(",")[1]);
        double longStart = Double.parseDouble(longitude.split(",")[0]);
        double longEnd = Double.parseDouble(longitude.split(",")[1]);

        if (gardenTypeRequest == GardenTypeRequest.PUBLIC) {
            return gardenService.getPublicGardenByCoordinate(latStart, latEnd, longStart, longEnd);
        }

        if (gardenTypeRequest == GardenTypeRequest.RPIVATE) {
            return gardenService.getPrivateGardenByCoordinate(latStart, latEnd, longStart, longEnd);
        }

        return gardenService.getAllGardenByCoordinate(latStart, latEnd, longStart, longEnd);
    }

    @GetMapping("recent")
    public List<GardenResponse> getRecentlyViewedGarden(@MemberId Long memberId,
                                                        @RequestParam(value = "page", required = false) Integer page,
                                                        @RequestParam(value = "size", required = false) Integer size) {
        if (page == null) page = 1;
        if (size == null) size = 10;

        return gardenViewService
                .getRecentGardenView(memberId, page - 1, size).stream()
                .map(GardenResponse::of)
                .collect(Collectors.toList());
    }

    @GetMapping("mine")
    public List<GardenPostResponse> getMyGarden(@MemberId Long memberId) {
        return gardenService.getGardenByMemberId(memberId);
    }

    @GetMapping("{gardenId}")
    public GardenDetailResponse getGardenDetail(@MemberId Long memberId,
                                                @PathVariable("gardenId") Long gardenId) {
        return gardenService.getGardenDetailByGardenId(memberId, gardenId);
    }

    @PostMapping
    public GardenAddSuccessResponse addGarden(@MemberId Long memberId,
                                              @RequestBody @Valid GardenPostAddRequest gardenAddRequest) {
        Garden garden = gardenService.addGarden(gardenAddRequest, memberId);

        return GardenAddSuccessResponse.builder()
                .garden(GardenResponse.of(garden))
                .build();
    }

    @SneakyThrows
    @PostMapping("images")
    public ImageUploadSuccessResponse uploadImage(@RequestParam("file") MultipartFile file) {
        String uuidUntil5 = UUID.randomUUID().toString().substring(0, 5);
        String fileName = uuidUntil5 + (file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replaceAll(" ", ""));
        byte[] fileBytes = file.getInputStream().readAllBytes();

        String imageUrl = s3Service.putObject(fileName, fileBytes);

        return ImageUploadSuccessResponse.builder()
                .id(uuidUntil5)
                .imageUrl(imageUrl)
                .build();
    }

    @PutMapping("{gardenId}")
    public ResponseEntity<GardenResponse> editGarden(@PathVariable("gardenId") Long gardenId) {
        return null;
    }

    @PatchMapping("{gardenId}")
    public ResponseEntity<GardenResponse> editGardenSelectively(@PathVariable("gardenId") Long gardenId) {
        return null;
    }

    @DeleteMapping("{gardenId}")
    public ResponseEntity<String> deleteGarden(@MemberId Long memberId,
                                               @PathVariable("gardenId") Long gardenId) {
        gardenService.deleteGardenPost(memberId, gardenId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("텃밭을 지웠어요");
    }

}