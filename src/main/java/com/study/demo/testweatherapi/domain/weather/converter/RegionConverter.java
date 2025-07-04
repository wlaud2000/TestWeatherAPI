package com.study.demo.testweatherapi.domain.weather.converter;

import com.study.demo.testweatherapi.domain.weather.dto.request.RegionReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegionConverter {

    /**
     * CreateRegion 요청 DTO를 Region 엔티티로 변환
     * 격자 좌표는 기상청 API 호출 후 설정
     */
    public static Region toEntity(RegionReqDTO.CreateRegion request,
                                  BigDecimal gridX, BigDecimal gridY) {
        return Region.builder()
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .gridX(gridX)
                .gridY(gridY)
                .regCode(request.regCode())
                .build();
    }

    /**
     * Region 엔티티를 RegionInfo DTO로 변환
     */
    public static RegionResDTO.RegionInfo toRegionInfo(Region region) {
        return RegionResDTO.RegionInfo.builder()
                .regionId(region.getId())
                .name(region.getName())
                .latitude(region.getLatitude())
                .longitude(region.getLongitude())
                .gridX(region.getGridX())
                .gridY(region.getGridY())
                .regCode(region.getRegCode())
                .createdAt(region.getCreatedAt())
                .updatedAt(region.getUpdatedAt())
                .build();
    }

    /**
     * Region 엔티티를 CreateRegionResponse DTO로 변환
     */
    public static RegionResDTO.CreateRegionResponse toCreateResponse(Region region) {
        return RegionResDTO.CreateRegionResponse.builder()
                .regionId(region.getId())
                .name(region.getName())
                .latitude(region.getLatitude())
                .longitude(region.getLongitude())
                .gridX(region.getGridX())
                .gridY(region.getGridY())
                .regCode(region.getRegCode())
                .message("지역이 성공적으로 등록되었습니다.")
                .build();
    }

    /**
     * Region 엔티티 리스트를 RegionList DTO로 변환
     */
    public static RegionResDTO.RegionList toRegionList(List<Region> regions) {
        List<RegionResDTO.RegionInfo> regionInfos = regions.stream()
                .map(RegionConverter::toRegionInfo)
                .toList();

        return RegionResDTO.RegionList.builder()
                .regions(regionInfos)
                .totalCount(regions.size())
                .build();
    }

    /**
     * 좌표 변환 결과를 CoordinateConversionResponse DTO로 변환
     */
    public static RegionResDTO.CoordinateConversion toCoordinateConversionResponse(
            BigDecimal inputLatitude, BigDecimal inputLongitude,
            BigDecimal gridX, BigDecimal gridY) {
        return RegionResDTO.CoordinateConversion.builder()
                .inputLatitude(inputLatitude)
                .inputLongitude(inputLongitude)
                .gridX(gridX)
                .gridY(gridY)
                .message("좌표 변환이 성공적으로 완료되었습니다.")
                .build();
    }

    /**
     * Region 엔티티 리스트를 RegionSearchResult DTO로 변환
     */
    public static RegionResDTO.RegionSearchResult toSearchResult(List<Region> regions, String keyword) {
        List<RegionResDTO.RegionInfo> regionInfos = regions.stream()
                .map(RegionConverter::toRegionInfo)
                .toList();

        return RegionResDTO.RegionSearchResult.builder()
                .regions(regionInfos)
                .keyword(keyword)
                .resultCount(regions.size())
                .build();
    }

    /**
     * Region 엔티티를 RegionSimple DTO로 변환 (선택 목록용)
     */
    public static RegionResDTO.RegionSimple toRegionSimple(Region region) {
        return RegionResDTO.RegionSimple.builder()
                .regionId(region.getId())
                .name(region.getName())
                .regCode(region.getRegCode())
                .build();
    }

    /**
     * Region 엔티티를 DeleteRegionResponse DTO로 변환
     */
    public static RegionResDTO.DeleteRegionResponse toDeleteResponse(Region region) {
        return RegionResDTO.DeleteRegionResponse.builder()
                .regionId(region.getId())
                .name(region.getName())
                .message("지역이 성공적으로 삭제되었습니다.")
                .build();
    }
}
