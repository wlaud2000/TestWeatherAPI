package com.study.demo.testweatherapi.domain.weather.converter;

import com.study.demo.testweatherapi.domain.weather.dto.request.RegionReqDTO;
import com.study.demo.testweatherapi.domain.weather.dto.response.RegionResDTO;
import com.study.demo.testweatherapi.domain.weather.entity.Region;
import com.study.demo.testweatherapi.domain.weather.entity.RegionCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegionConverter {

    // ==== RegionCode 관련 변환 메서드들 ====

    /**
     * CreateRegionCode 요청 DTO를 RegionCode 엔티티로 변환
     */
    public static RegionCode toRegionCodeEntity(RegionReqDTO.CreateRegionCode request) {
        return RegionCode.builder()
                .landRegCode(request.landRegCode())
                .tempRegCode(request.tempRegCode())
                .name(request.name())
                .build();
    }

    /**
     * RegionCode 엔티티를 RegionCodeInfo DTO로 변환
     */
    public static RegionResDTO.RegionCodeInfo toRegionCodeInfo(RegionCode regionCode) {
        return RegionResDTO.RegionCodeInfo.builder()
                .regionCodeId(regionCode.getId())
                .landRegCode(regionCode.getLandRegCode())
                .tempRegCode(regionCode.getTempRegCode())
                .name(regionCode.getName())
                .build();
    }

    /**
     * RegionCode 엔티티를 RegionCodeDetail DTO로 변환
     */
    public static RegionResDTO.RegionCodeDetail toRegionCodeDetail(RegionCode regionCode, int regionCount) {
        return RegionResDTO.RegionCodeDetail.builder()
                .regionCodeId(regionCode.getId())
                .landRegCode(regionCode.getLandRegCode())
                .tempRegCode(regionCode.getTempRegCode())
                .name(regionCode.getName())
                .regionCount(regionCount)
                .createdAt(regionCode.getCreatedAt())
                .updatedAt(regionCode.getUpdatedAt())
                .build();
    }

    /**
     * RegionCode 엔티티를 CreateRegionCodeResponse DTO로 변환
     */
    public static RegionResDTO.CreateRegionCodeResponse toCreateRegionCodeResponse(RegionCode regionCode) {
        return RegionResDTO.CreateRegionCodeResponse.builder()
                .regionCodeId(regionCode.getId())
                .landRegCode(regionCode.getLandRegCode())
                .tempRegCode(regionCode.getTempRegCode())
                .name(regionCode.getName())
                .message("지역코드가 성공적으로 등록되었습니다.")
                .build();
    }

    /**
     * RegionCode 리스트를 RegionCodeList DTO로 변환
     */
    public static RegionResDTO.RegionCodeList toRegionCodeList(List<Object[]> regionCodesWithCount) {
        List<RegionResDTO.RegionCodeDetail> regionCodeDetails = regionCodesWithCount.stream()
                .map(result -> {
                    RegionCode regionCode = (RegionCode) result[0];
                    Long regionCount = (Long) result[1];
                    return toRegionCodeDetail(regionCode, regionCount.intValue());
                })
                .toList();

        return RegionResDTO.RegionCodeList.builder()
                .regionCodes(regionCodeDetails)
                .totalCount(regionCodeDetails.size())
                .build();
    }

    // ==== Region 관련 변환 메서드들 ====

    /**
     * CreateRegion 요청 DTO를 Region 엔티티로 변환
     * 격자 좌표는 기상청 API 호출 후 설정
     */
    public static Region toEntity(RegionReqDTO.CreateRegion request,
                                  BigDecimal gridX, BigDecimal gridY, RegionCode regionCode) {
        return Region.builder()
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .gridX(gridX)
                .gridY(gridY)
                .regionCode(regionCode)
                .build();
    }

    /**
     * CreateRegionWithNewCode 요청으로 Region 엔티티 생성
     */
    public static Region toEntityWithNewCode(RegionReqDTO.CreateRegionWithNewCode request,
                                             BigDecimal gridX, BigDecimal gridY, RegionCode regionCode) {
        return Region.builder()
                .name(request.name())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .gridX(gridX)
                .gridY(gridY)
                .regionCode(regionCode)
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
                .regionCode(toRegionCodeInfo(region.getRegionCode()))
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
                .regionCode(toRegionCodeInfo(region.getRegionCode()))
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
                .landRegCode(region.getRegionCode().getLandRegCode())
                .tempRegCode(region.getRegionCode().getTempRegCode())
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

    /**
     * RegionCode 엔티티를 DeleteRegionCodeResponse DTO로 변환
     */
    public static RegionResDTO.DeleteRegionCodeResponse toDeleteRegionCodeResponse(RegionCode regionCode) {
        return RegionResDTO.DeleteRegionCodeResponse.builder()
                .regionCodeId(regionCode.getId())
                .name(regionCode.getName())
                .message("지역코드가 성공적으로 삭제되었습니다.")
                .build();
    }

    /**
     * 지역코드별 지역 목록 응답 생성
     */
    public static RegionResDTO.RegionsByCodeResponse toRegionsByCodeResponse(
            RegionCode regionCode, List<Region> regions) {
        List<RegionResDTO.RegionSimple> regionSimples = regions.stream()
                .map(RegionConverter::toRegionSimple)
                .toList();

        return RegionResDTO.RegionsByCodeResponse.builder()
                .regionCode(toRegionCodeInfo(regionCode))
                .regions(regionSimples)
                .regionCount(regions.size())
                .build();
    }
}
