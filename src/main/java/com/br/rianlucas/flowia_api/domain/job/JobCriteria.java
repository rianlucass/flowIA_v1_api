package com.br.rianlucas.flowia_api.domain.job;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobCriteria {
    private RequiredCriteria required;

    private DesiredCriteria desired;

    private EliminatoryCriteria eliminatory;

    private WeightCriteria weights;

    private PositiveCriteria positive;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightCriteria {

        private Integer activities;

        private Integer experience;

        private Integer education;

        private Integer location;

        private Integer stability;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequiredCriteria {

        private List<String> skills;

        private List<String> activities;

        private List<String> education;

        private Integer minimumExperienceYears;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesiredCriteria {

        private List<String> courses;

        private List<String> experiences;

        private List<String> differentials;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EliminatoryCriteria {

        private Boolean requiredDegree;

        private Integer maxDistanceKm;

        private Integer minimumExperienceYears;

        private String requiredSchedule;

        private List<String> mandatorySkills;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositiveCriteria {

        private Boolean hasCertifications;

        private Integer jobStabilityYears;

        private Boolean hasLeadershipExperience;
    }
}
