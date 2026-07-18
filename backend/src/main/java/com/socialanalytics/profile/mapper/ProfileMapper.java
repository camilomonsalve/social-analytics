package com.socialanalytics.profile.mapper;

import com.socialanalytics.profile.dto.ProfileResponse;
import com.socialanalytics.profile.entity.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    ProfileResponse toResponse(Profile profile);
}
